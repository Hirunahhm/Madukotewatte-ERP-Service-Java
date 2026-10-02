package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employee.PaymentSummaryResponse;
import com.madukotawatte.erp.dto.payroll.ActiveLoanInfo;
import com.madukotawatte.erp.dto.payroll.PayrollPreviewRow;
import com.madukotawatte.erp.dto.payroll.PayrollSummaryResponse;
import com.madukotawatte.erp.dto.payroll.PayrollSummaryRow;
import com.madukotawatte.erp.dto.payroll.PayslipLabourLine;
import com.madukotawatte.erp.dto.payroll.PayslipRequest;
import com.madukotawatte.erp.dto.payroll.PayslipResponse;
import com.madukotawatte.erp.dto.payroll.PayslipSummaryResponse;
import com.madukotawatte.erp.dto.payroll.PayslipTapLine;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;
import com.madukotawatte.erp.entity.EmployeeLoanRepayment;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.Labour;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.entity.PayType;
import com.madukotawatte.erp.entity.Payslip;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.repository.EmployeeLoanRepaymentRepository;
import com.madukotawatte.erp.repository.EmployeeLoanRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import com.madukotawatte.erp.repository.LabourRepository;
import com.madukotawatte.erp.repository.PayslipRepository;
import com.madukotawatte.erp.service.support.PayrollCalculator;
import com.madukotawatte.erp.service.support.PayrollCalculator.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Payroll: preview of what is owed, payslips (earnings recomputed server-side, deductions validated, ledger +
 * expense + loan + advance bookkeeping in one transaction), voiding, and the per-employee summary reports use.
 *
 * Booking per payslip: system expense "Salaries &amp; Wages" for GROSS, ledger SALARY_PAYMENT for NET (the expense
 * is linked to that ledger row, never a second posting); the advance recovery / loan deduction clear employee
 * receivables, so gross = net + advance + loan + other deduction keeps the books balanced.
 */
@Service
@RequiredArgsConstructor
public class PayrollService {

    public static final String REF_PAYSLIP = "PAYSLIP";
    public static final String EXPENSE_TYPE = "Salaries & Wages";
    private static final long MAX_RANGE_DAYS = 800;

    private final EmployeeRepository employeeRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final LabourRepository labourRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeLoanRepaymentRepository loanRepaymentRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeFinanceService employeeFinanceService;
    private final ExpenseService expenseService;
    private final LedgerService ledgerService;

    /** What the server computes as earned for one employee in a period. */
    private record Earnings(BigDecimal fixed, BigDecimal tap, List<EmployeeTransaction> taps,
                            BigDecimal hourly, BigDecimal hours, List<Labour> labours) {
        BigDecimal total() {
            return fixed.add(tap).add(hourly);
        }
    }

    // -- Preview --------------------------------------------------------------

    /** Per ACTIVE employee (or just {@code employeeId}, active or not): earnings, suggested deductions, net. */
    @Transactional(readOnly = true)
    public List<PayrollPreviewRow> preview(LocalDate from, LocalDate to, String employeeId) {
        validateRange(from, to);
        List<Employee> employees = employeeId != null && !employeeId.isBlank()
                ? List.of(findEmployee(employeeId))
                : employeeRepository.findByIsActiveTrue();
        Map<String, BigDecimal> advances = outstandingAdvanceMap();
        Map<String, List<EmployeeLoan>> loansByEmployee = new HashMap<>();
        for (EmployeeLoan loan : employeeLoanRepository.findByIsActiveTrue()) {
            loansByEmployee.computeIfAbsent(loan.getEmployee().getEmployeeId(), k -> new ArrayList<>()).add(loan);
        }
        return employees.stream()
                .sorted(Comparator.comparing(Employee::getName, String.CASE_INSENSITIVE_ORDER))
                .map(e -> buildRow(e, from, to, advances.getOrDefault(e.getEmployeeId(), BigDecimal.ZERO),
                        loansByEmployee.getOrDefault(e.getEmployeeId(), List.of())))
                .toList();
    }

    private PayrollPreviewRow buildRow(Employee e, LocalDate from, LocalDate to, BigDecimal outstanding,
                                       List<EmployeeLoan> activeLoans) {
        List<Payslip> overlapping = payslipRepository.findPaidOverlapping(e.getEmployeeId(), from, to);
        Earnings earnings = computeEarnings(e, from, to, overlapping);
        BigDecimal gross = PayrollCalculator.money(earnings.total());

        EmployeeLoan loan = activeLoans.isEmpty() ? null : activeLoans.get(0);
        PayrollCalculator.Deductions suggested = PayrollCalculator.suggestDeductions(gross, outstanding,
                loan != null ? loan.getCurrentBalance() : null, loan != null ? loan.getInstallment() : null);

        return PayrollPreviewRow.builder()
                .employeeId(e.getEmployeeId())
                .employeeName(e.getName())
                .position(e.getPosition())
                .payType(e.getPayType())
                .salary(e.getSalary())
                .ratePerTree(e.getRatePerTree())
                .hourlyRate(e.getHourlyRate())
                .fixedSalary(earnings.fixed())
                .tapEarnings(earnings.tap())
                .tapRecordCount(earnings.taps().size())
                .hourlyEarnings(earnings.hourly())
                .labourHours(earnings.hours())
                .grossPay(gross)
                .outstandingAdvances(PayrollCalculator.money(outstanding))
                .suggestedAdvanceDeduction(suggested.advance())
                .activeLoan(loan == null ? null : ActiveLoanInfo.builder().loanId(loan.getLoanId())
                        .balance(loan.getCurrentBalance()).installment(loan.getInstallment()).build())
                .suggestedLoanDeduction(suggested.loan())
                .netPay(suggested.net())
                .alreadyPaid(overlapping.stream().map(this::toSummary).toList())
                .build();
    }

    private Earnings computeEarnings(Employee e, LocalDate from, LocalDate to, List<Payslip> overlapping) {
        BigDecimal fixed = PayrollCalculator.money(BigDecimal.ZERO);
        if (e.getPayType() == PayType.FIXED_MONTHLY) {
            List<DateRange> covered = overlapping.stream()
                    .filter(p -> p.getFixedSalary().signum() > 0)
                    .map(p -> new DateRange(p.getPeriodStart(), p.getPeriodEnd()))
                    .toList();
            fixed = PayrollCalculator.fixedSalary(e.getSalary(), from, to, e.getJoinedDate(), covered);
        }
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<EmployeeTransaction> taps = employeeTransactionRepository.findUnpaidTaps(e.getEmployeeId(), start, end);
        BigDecimal tap = PayrollCalculator.money(taps.stream().map(EmployeeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        List<Labour> labours = labourRepository.findUnpaid(e.getEmployeeId(), start, end);
        BigDecimal hourly = PayrollCalculator.money(labours.stream().map(Labour::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal hours = labours.stream().map(Labour::getWorkedHours).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Earnings(fixed, tap, taps, hourly, hours, labours);
    }

    private Map<String, BigDecimal> outstandingAdvanceMap() {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : employeeTransactionRepository.sumByTypes(
                List.of(EmployeeFinanceService.TYPE_ADVANCE, EmployeeFinanceService.TYPE_ADVANCE_RECOVERY))) {
            BigDecimal amount = (BigDecimal) row[2];
            boolean advance = EmployeeFinanceService.TYPE_ADVANCE.equals(row[1]);
            map.merge((String) row[0], advance ? amount : amount.negate(), BigDecimal::add);
        }
        map.replaceAll((k, v) -> v.max(BigDecimal.ZERO));
        return map;
    }

    // -- Create ---------------------------------------------------------------

    @Transactional
    public PayslipResponse createPayslip(PayslipRequest request) {
        return toResponse(createInternal(request), true);
    }

    /** All-or-nothing: any failure rolls every payslip back. */
    @Transactional
    public List<PayslipResponse> createBulk(List<PayslipRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one payslip is required");
        }
        List<PayslipResponse> created = new ArrayList<>();
        for (PayslipRequest request : requests) {
            created.add(toResponse(createInternal(request), true));
        }
        return created;
    }

    private Payslip createInternal(PayslipRequest r) {
        requireFields(r);
        validateRange(r.getPeriodStart(), r.getPeriodEnd());
        Employee employee = findEmployee(r.getEmployeeId());
        ledgerService.requireCashOrBank(r.getAccountCode());

        List<Payslip> overlapping = payslipRepository.findPaidOverlapping(employee.getEmployeeId(),
                r.getPeriodStart(), r.getPeriodEnd());
        Earnings earnings = computeEarnings(employee, r.getPeriodStart(), r.getPeriodEnd(), overlapping);

        BigDecimal other = nonNegative(r.getOtherEarnings(), "Other earnings");
        BigDecimal advance = nonNegative(r.getAdvanceDeduction(), "Advance deduction");
        BigDecimal loanDed = nonNegative(r.getLoanDeduction(), "Loan deduction");
        BigDecimal otherDed = nonNegative(r.getOtherDeduction(), "Other deduction");

        BigDecimal gross = PayrollCalculator.money(earnings.total().add(other));
        if (gross.signum() <= 0) {
            throw new BadRequestException("Nothing to pay " + employee.getName() + " for this period");
        }
        BigDecimal outstanding = employeeFinanceService.outstandingAdvances(employee.getEmployeeId());
        if (advance.compareTo(outstanding) > 0) {
            throw new BadRequestException(employee.getName() + ": advance deduction exceeds the outstanding advances ("
                    + PayrollCalculator.money(outstanding) + ")");
        }
        EmployeeLoan loan = null;
        if (loanDed.signum() > 0) {
            List<EmployeeLoan> active = employeeLoanRepository.findByEmployee_EmployeeIdAndIsActiveTrue(employee.getEmployeeId());
            if (active.isEmpty()) {
                throw new BadRequestException(employee.getName() + " has no active loan to deduct from");
            }
            loan = active.get(0);
            if (loanDed.compareTo(loan.getCurrentBalance()) > 0) {
                throw new BadRequestException(employee.getName() + ": loan deduction exceeds the loan balance ("
                        + loan.getCurrentBalance() + ")");
            }
        }
        BigDecimal net = PayrollCalculator.net(gross, advance, loanDed, otherDed);
        if (net.signum() < 0) {
            throw new BadRequestException(employee.getName() + ": deductions exceed the gross pay of " + gross);
        }

        Payslip payslip = payslipRepository.save(Payslip.builder()
                .payslipId(UUID.randomUUID().toString())
                .employee(employee)
                .periodStart(r.getPeriodStart())
                .periodEnd(r.getPeriodEnd())
                .fixedSalary(earnings.fixed())
                .tapEarnings(earnings.tap())
                .hourlyEarnings(earnings.hourly())
                .otherEarnings(PayrollCalculator.money(other))
                .otherEarningsNote(r.getOtherEarningsNote())
                .grossPay(gross)
                .advanceDeduction(PayrollCalculator.money(advance))
                .loanDeduction(PayrollCalculator.money(loanDed))
                .otherDeduction(PayrollCalculator.money(otherDed))
                .otherDeductionNote(r.getOtherDeductionNote())
                .netPay(net)
                .accountCode(r.getAccountCode())
                .paymentDate(r.getPaymentDate())
                .loan(loan)
                .notes(r.getNotes())
                .status(Payslip.PAID)
                .build());
        LocalDateTime paidAt = r.getPaymentDate().atStartOfDay();

        // settle the earnings that are now on this payslip
        for (EmployeeTransaction tap : earnings.taps()) {
            tap.setPayslip(payslip);
        }
        employeeTransactionRepository.saveAll(earnings.taps());
        for (Labour labour : earnings.labours()) {
            labour.setPayslip(payslip);
            labour.setIsPaid(true);
            labour.setAccountCode(r.getAccountCode());
        }
        labourRepository.saveAll(earnings.labours());

        // recoveries
        if (advance.signum() > 0) {
            employeeTransactionRepository.save(EmployeeTransaction.builder()
                    .transactionRecordId(UUID.randomUUID().toString())
                    .employee(employee)
                    .type(EmployeeFinanceService.TYPE_ADVANCE_RECOVERY)
                    .amount(PayrollCalculator.money(advance))
                    .timestamp(paidAt)
                    .payslip(payslip)
                    .note("Recovered on payslip")
                    .build());
        }
        if (loan != null) {
            employeeTransactionRepository.save(EmployeeTransaction.builder()
                    .transactionRecordId(UUID.randomUUID().toString())
                    .employee(employee)
                    .type(EmployeeFinanceService.TYPE_LOAN_PAYMENT)
                    .amount(PayrollCalculator.money(loanDed))
                    .timestamp(paidAt)
                    .payslip(payslip)
                    .note("Loan deduction on payslip")
                    .build());
            loanRepaymentRepository.save(EmployeeLoanRepayment.builder()
                    .repaymentId(UUID.randomUUID().toString())
                    .loan(loan)
                    .amount(PayrollCalculator.money(loanDed))
                    .repaymentDate(r.getPaymentDate())
                    .source(EmployeeLoanRepayment.SOURCE_PAYSLIP)
                    .payslip(payslip)
                    .build());
            employeeFinanceService.applyToLoanBalance(loan, loanDed.negate());
            employeeLoanRepository.save(loan);
        }

        // money: NET leaves the account; the GROSS expense is linked to that same ledger row
        MonetaryAssetTransaction ledgerRow = null;
        if (net.signum() > 0) {
            ledgerRow = ledgerService.post(r.getAccountCode(), LedgerDirection.OUT, net, r.getPaymentDate(),
                    LedgerCategory.SALARY_PAYMENT, REF_PAYSLIP, payslip.getPayslipId(),
                    "Payslip: " + employee.getName() + " (" + r.getPeriodStart() + " to " + r.getPeriodEnd() + ")");
            payslip.setTransaction(ledgerRow);
        }
        expenseService.recordSystemExpenseLinkedToLedger(EXPENSE_TYPE, gross, paidAt, r.getAccountCode(),
                ledgerRow, REF_PAYSLIP, payslip.getPayslipId());
        return payslipRepository.save(payslip);
    }

    private void requireFields(PayslipRequest r) {
        if (r.getEmployeeId() == null || r.getEmployeeId().isBlank()) throw new BadRequestException("employeeId is required");
        if (r.getPeriodStart() == null || r.getPeriodEnd() == null) throw new BadRequestException("periodStart and periodEnd are required");
        if (r.getAccountCode() == null || r.getAccountCode().isBlank()) throw new BadRequestException("accountCode is required");
        if (r.getPaymentDate() == null) throw new BadRequestException("paymentDate is required");
    }

    private BigDecimal nonNegative(BigDecimal value, String label) {
        if (value == null) return BigDecimal.ZERO;
        if (value.signum() < 0) throw new BadRequestException(label + " cannot be negative");
        return value;
    }

    // -- Void -----------------------------------------------------------------

    /** Full undo of a payslip: ledger, expense, links, recoveries, loan balance. The payslip row is kept as VOID. */
    @Transactional
    public PayslipResponse voidPayslip(String id, String reason) {
        Payslip payslip = findPayslip(id);
        if (!Payslip.PAID.equals(payslip.getStatus())) {
            throw new BadRequestException("This payslip is already void");
        }
        String why = reason != null && !reason.isBlank() ? reason.trim() : "Payslip voided";

        List<EmployeeLoanRepayment> repayments = loanRepaymentRepository.findByPayslip_PayslipId(id);
        for (EmployeeLoanRepayment repayment : repayments) {
            if (!Boolean.TRUE.equals(repayment.getLoan().getIsActive())) {
                employeeFinanceService.ensureNoOtherActiveLoan(repayment.getLoan());
            }
        }

        // expense (and the SALARY_PAYMENT ledger row it is linked to), then the row itself if it had no expense
        expenseService.removeSystemExpenses(REF_PAYSLIP, id, why);
        ledgerService.reverseIfActive(payslip.getTransaction(), why);

        List<EmployeeTransaction> linked = employeeTransactionRepository.findByPayslip_PayslipId(id);
        List<EmployeeTransaction> toDelete = new ArrayList<>();
        for (EmployeeTransaction t : linked) {
            if ("Latex_Tap".equals(t.getType())) {
                t.setPayslip(null);
                employeeTransactionRepository.save(t);
            } else {
                toDelete.add(t);
            }
        }
        for (Labour labour : labourRepository.findByPayslip_PayslipId(id)) {
            labour.setPayslip(null);
            labour.setIsPaid(false);
            labour.setAccountCode(null);
            labourRepository.save(labour);
        }
        for (EmployeeLoanRepayment repayment : repayments) {
            EmployeeLoan loan = repayment.getLoan();
            employeeFinanceService.applyToLoanBalance(loan, repayment.getAmount());
            employeeLoanRepository.save(loan);
        }
        loanRepaymentRepository.deleteAll(repayments);
        employeeTransactionRepository.deleteAll(toDelete);

        payslip.setStatus(Payslip.VOID);
        payslip.setVoidReason(why.length() > 255 ? why.substring(0, 255) : why);
        payslip.setVoidedAt(LocalDateTime.now());
        return toResponse(payslipRepository.save(payslip), true);
    }

    // -- Reads ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<PayslipResponse> listPayslips(LocalDate from, LocalDate to, String employeeId, Pageable pageable) {
        return PageResponse.from(payslipRepository.search(
                employeeId != null && !employeeId.isBlank() ? employeeId : null, from, to, pageable)
                .map(p -> toResponse(p, false)));
    }

    @Transactional(readOnly = true)
    public PayslipResponse getPayslip(String id) {
        return toResponse(findPayslip(id), true);
    }

    /** PAID payslips with payment_date in [from, to], per employee and in total (Wave 3 reports reuse this). */
    @Transactional(readOnly = true)
    public PayrollSummaryResponse summary(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("A valid from/to range is required");
        }
        Map<String, PayrollSummaryRow> rows = new LinkedHashMap<>();
        for (Payslip p : payslipRepository.findPaidByPaymentDate(from, to)) {
            PayrollSummaryRow row = rows.computeIfAbsent(p.getEmployee().getEmployeeId(), id -> emptyRow(id, p.getEmployee().getName()));
            accumulate(row, p);
        }
        PayrollSummaryRow totals = emptyRow(null, "Total");
        rows.values().forEach(row -> {
            totals.setPayslipCount(totals.getPayslipCount() + row.getPayslipCount());
            totals.setGross(totals.getGross().add(row.getGross()));
            totals.setFixedSalary(totals.getFixedSalary().add(row.getFixedSalary()));
            totals.setTap(totals.getTap().add(row.getTap()));
            totals.setHourly(totals.getHourly().add(row.getHourly()));
            totals.setOther(totals.getOther().add(row.getOther()));
            totals.setAdvanceRecovered(totals.getAdvanceRecovered().add(row.getAdvanceRecovered()));
            totals.setLoanDeducted(totals.getLoanDeducted().add(row.getLoanDeducted()));
            totals.setOtherDeducted(totals.getOtherDeducted().add(row.getOtherDeducted()));
            totals.setNet(totals.getNet().add(row.getNet()));
        });
        List<PayrollSummaryRow> sorted = rows.values().stream()
                .sorted(Comparator.comparing(PayrollSummaryRow::getEmployeeName, String.CASE_INSENSITIVE_ORDER)).toList();
        return PayrollSummaryResponse.builder().from(from).to(to).rows(sorted).totals(totals).build();
    }

    private PayrollSummaryRow emptyRow(String employeeId, String name) {
        BigDecimal z = PayrollCalculator.money(BigDecimal.ZERO);
        return PayrollSummaryRow.builder().employeeId(employeeId).employeeName(name).payslipCount(0)
                .gross(z).fixedSalary(z).tap(z).hourly(z).other(z).advanceRecovered(z).loanDeducted(z)
                .otherDeducted(z).net(z).build();
    }

    private void accumulate(PayrollSummaryRow row, Payslip p) {
        row.setPayslipCount(row.getPayslipCount() + 1);
        row.setGross(row.getGross().add(p.getGrossPay()));
        row.setFixedSalary(row.getFixedSalary().add(p.getFixedSalary()));
        row.setTap(row.getTap().add(p.getTapEarnings()));
        row.setHourly(row.getHourly().add(p.getHourlyEarnings()));
        row.setOther(row.getOther().add(p.getOtherEarnings()));
        row.setAdvanceRecovered(row.getAdvanceRecovered().add(p.getAdvanceDeduction()));
        row.setLoanDeducted(row.getLoanDeducted().add(p.getLoanDeduction()));
        row.setOtherDeducted(row.getOtherDeducted().add(p.getOtherDeduction()));
        row.setNet(row.getNet().add(p.getNetPay()));
    }

    /** KPI cards of the Payroll screen. */
    @Transactional(readOnly = true)
    public PaymentSummaryResponse paymentSummary() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        BigDecimal totalSalaryCost = employeeRepository.findByIsActiveTrue().stream()
                .filter(e -> e.getPayType() == PayType.FIXED_MONTHLY && e.getSalary() != null)
                .map(Employee::getSalary).reduce(BigDecimal.ZERO, BigDecimal::add);

        PayrollSummaryRow paid = summary(monthStart, monthEnd).getTotals();
        // month to date, so historic pre-payslip earnings never inflate the figure
        BigDecimal unpaid = preview(monthStart, today, null).stream().map(PayrollPreviewRow::getGrossPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal advances = outstandingAdvanceMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal loans = employeeLoanRepository.findByIsActiveTrue().stream().map(EmployeeLoan::getCurrentBalance)
                .filter(java.util.Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

        return PaymentSummaryResponse.builder()
                .totalSalaryCost(totalSalaryCost)
                .payrollPaidGross(paid.getGross())
                .payrollPaidNet(paid.getNet())
                .unpaidEarnings(PayrollCalculator.money(unpaid))
                .outstandingAdvances(PayrollCalculator.money(advances))
                .activeEmployeeLoansTotal(loans)
                .build();
    }

    // -- Mapping --------------------------------------------------------------

    private PayslipSummaryResponse toSummary(Payslip p) {
        return PayslipSummaryResponse.builder()
                .payslipId(p.getPayslipId())
                .employeeId(p.getEmployee().getEmployeeId())
                .employeeName(p.getEmployee().getName())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .paymentDate(p.getPaymentDate())
                .fixedSalary(p.getFixedSalary())
                .grossPay(p.getGrossPay())
                .netPay(p.getNetPay())
                .accountCode(p.getAccountCode())
                .status(p.getStatus())
                .build();
    }

    private PayslipResponse toResponse(Payslip p, boolean withLines) {
        List<PayslipTapLine> tapLines = null;
        List<PayslipLabourLine> labourLines = null;
        if (withLines) {
            tapLines = employeeTransactionRepository.findByPayslip_PayslipId(p.getPayslipId()).stream()
                    .filter(t -> "Latex_Tap".equals(t.getType()))
                    .sorted(Comparator.comparing(EmployeeTransaction::getTimestamp))
                    .map(t -> PayslipTapLine.builder().transactionRecordId(t.getTransactionRecordId())
                            .date(t.getTimestamp().toLocalDate()).amount(t.getAmount()).build())
                    .toList();
            labourLines = labourRepository.findByPayslip_PayslipId(p.getPayslipId()).stream()
                    .sorted(Comparator.comparing(Labour::getTimestamp))
                    .map(l -> PayslipLabourLine.builder().labourId(l.getLabourId()).date(l.getTimestamp().toLocalDate())
                            .workType(l.getWorkType()).workedHours(l.getWorkedHours()).hourlyRate(l.getHourlyRate())
                            .amount(l.getAmount()).build())
                    .toList();
        }
        return PayslipResponse.builder()
                .payslipId(p.getPayslipId())
                .employeeId(p.getEmployee().getEmployeeId())
                .employeeName(p.getEmployee().getName())
                .position(p.getEmployee().getPosition())
                .periodStart(p.getPeriodStart())
                .periodEnd(p.getPeriodEnd())
                .fixedSalary(p.getFixedSalary())
                .tapEarnings(p.getTapEarnings())
                .hourlyEarnings(p.getHourlyEarnings())
                .otherEarnings(p.getOtherEarnings())
                .otherEarningsNote(p.getOtherEarningsNote())
                .grossPay(p.getGrossPay())
                .advanceDeduction(p.getAdvanceDeduction())
                .loanDeduction(p.getLoanDeduction())
                .otherDeduction(p.getOtherDeduction())
                .otherDeductionNote(p.getOtherDeductionNote())
                .netPay(p.getNetPay())
                .accountCode(p.getAccountCode())
                .paymentDate(p.getPaymentDate())
                .ledgerTransactionId(p.getTransaction() != null ? p.getTransaction().getId() : null)
                .loanId(p.getLoan() != null ? p.getLoan().getLoanId() : null)
                .notes(p.getNotes())
                .status(p.getStatus())
                .voidReason(p.getVoidReason())
                .voidedAt(p.getVoidedAt())
                .tapLines(tapLines)
                .labourLines(labourLines)
                .createdAt(p.getCreatedAt())
                .build();
    }

    // -- Helpers --------------------------------------------------------------

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) throw new BadRequestException("from and to dates are required");
        if (to.isBefore(from)) throw new BadRequestException("The period end is before its start");
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) throw new BadRequestException("The period is too long");
    }

    private Employee findEmployee(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }

    private Payslip findPayslip(String id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payslip", "id", id));
    }
}
