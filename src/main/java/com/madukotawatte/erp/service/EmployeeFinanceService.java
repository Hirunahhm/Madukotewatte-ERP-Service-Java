package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.AdvanceRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.dto.loan.LoanRepaymentRequest;
import com.madukotawatte.erp.dto.loan.UpdateLoanRequest;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;
import com.madukotawatte.erp.entity.EmployeeLoanRepayment;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.EmployeeLoanMapper;
import com.madukotawatte.erp.mapper.EmployeeTransactionMapper;
import com.madukotawatte.erp.repository.EmployeeLoanRepaymentRepository;
import com.madukotawatte.erp.repository.EmployeeLoanRepository;
import com.madukotawatte.erp.repository.EmployeeRepository;
import com.madukotawatte.erp.repository.EmployeeTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Employee money events outside payroll: advances and employee loans (issue, cash repayment, undo). Every one
 * moves a real cash/bank balance through the ledger. Recoveries made through a payslip live in PayrollService.
 */
@Service
@RequiredArgsConstructor
public class EmployeeFinanceService {

    public static final String REF_EMPLOYEE_TRANSACTION = "EMPLOYEE_TRANSACTION";
    public static final String REF_EMPLOYEE_LOAN = "EMPLOYEE_LOAN";
    public static final String TYPE_ADVANCE = "Advance";
    public static final String TYPE_ADVANCE_RECOVERY = "Advance_Recovery";
    public static final String TYPE_LOAN_PAYMENT = "Loan_Payment";

    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeLoanRepaymentRepository loanRepaymentRepository;
    private final LedgerService ledgerService;

    // -- Advances ------------------------------------------------------------

    @Transactional
    public EmployeeTransactionResponse giveAdvance(AdvanceRequest request) {
        Employee employee = findEmployee(request.getEmployeeId());
        ledgerService.requireCashOrBank(request.getAccountCode());

        String id = UUID.randomUUID().toString();
        MonetaryAssetTransaction ledgerRow = ledgerService.post(request.getAccountCode(), LedgerDirection.OUT,
                request.getAmount(), request.getDate(), LedgerCategory.EMPLOYEE_ADVANCE,
                REF_EMPLOYEE_TRANSACTION, id, "Advance: " + employee.getName());

        EmployeeTransaction transaction = EmployeeTransaction.builder()
                .transactionRecordId(id)
                .employee(employee)
                .type(TYPE_ADVANCE)
                .accountCode(request.getAccountCode())
                .amount(request.getAmount())
                .timestamp(request.getDate().atStartOfDay())
                .transaction(ledgerRow)
                .note(request.getNote())
                .build();
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.save(transaction));
    }

    /**
     * Undoes an advance (reverses its ledger row). Recoveries are pooled per employee, so an advance can only be
     * removed while the employee's outstanding advances still cover it, i.e. none of it has been recovered.
     */
    @Transactional
    public void deleteAdvance(String id) {
        EmployeeTransaction advance = employeeTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeTransaction", "id", id));
        if (!TYPE_ADVANCE.equals(advance.getType())) {
            throw new BadRequestException("Only advances can be deleted here");
        }
        BigDecimal outstanding = outstandingAdvances(advance.getEmployee().getEmployeeId());
        if (outstanding.compareTo(advance.getAmount()) < 0) {
            throw new BadRequestException("This advance has already been (partly) recovered and cannot be deleted");
        }
        MonetaryAssetTransaction ledgerRow = advance.getTransaction();
        advance.setTransaction(null);
        employeeTransactionRepository.delete(advance);
        employeeTransactionRepository.flush();
        if (ledgerRow != null) {
            ledgerService.reverseIfActive(ledgerRow, "Advance deleted");
        }
    }

    /** Sum of Advance minus Advance_Recovery over all time. */
    public BigDecimal outstandingAdvances(String employeeId) {
        return employeeTransactionRepository.sumByEmployeeAndType(employeeId, TYPE_ADVANCE)
                .subtract(employeeTransactionRepository.sumByEmployeeAndType(employeeId, TYPE_ADVANCE_RECOVERY))
                .max(BigDecimal.ZERO);
    }

    // -- Employee loans ------------------------------------------------------

    @Transactional
    public EmployeeLoanResponse createLoan(EmployeeLoanRequest request) {
        Employee employee = findEmployee(request.getEmployeeId());
        if (!employeeLoanRepository.findByEmployee_EmployeeIdAndIsActiveTrue(request.getEmployeeId()).isEmpty()) {
            throw new BadRequestException("Employee already has an active loan");
        }
        ledgerService.requireCashOrBank(request.getAccountCode());

        EmployeeLoan loan = EmployeeLoanMapper.toEntity(request, employee);
        loan.setTransaction(ledgerService.post(request.getAccountCode(), LedgerDirection.OUT,
                request.getPrincipalAmount(), request.getIssuedDate(), LedgerCategory.EMPLOYEE_LOAN_ISSUE,
                REF_EMPLOYEE_LOAN, loan.getLoanId(), "Loan issued: " + employee.getName()));
        return toResponse(employeeLoanRepository.save(loan));
    }

    /** The employee repays part of the loan in cash/bank. */
    @Transactional
    public EmployeeLoanResponse repayLoan(String loanId, LoanRepaymentRequest request) {
        EmployeeLoan loan = findLoan(loanId);
        if (!Boolean.TRUE.equals(loan.getIsActive())) {
            throw new BadRequestException("This loan is already settled");
        }
        if (request.getAmount().compareTo(loan.getCurrentBalance()) > 0) {
            throw new BadRequestException("Repayment exceeds the loan balance of " + loan.getCurrentBalance());
        }
        ledgerService.requireCashOrBank(request.getAccountCode());

        MonetaryAssetTransaction ledgerRow = ledgerService.post(request.getAccountCode(), LedgerDirection.IN,
                request.getAmount(), request.getDate(), LedgerCategory.EMPLOYEE_LOAN_REPAYMENT,
                REF_EMPLOYEE_LOAN, loan.getLoanId(), "Loan repayment: " + loan.getEmployee().getName());
        loanRepaymentRepository.save(EmployeeLoanRepayment.builder()
                .repaymentId(UUID.randomUUID().toString())
                .loan(loan)
                .amount(request.getAmount())
                .repaymentDate(request.getDate())
                .source(EmployeeLoanRepayment.SOURCE_CASH)
                .transaction(ledgerRow)
                .build());

        applyToLoanBalance(loan, request.getAmount().negate());
        return toResponse(employeeLoanRepository.save(loan));
    }

    /** Undoes a cash repayment: reverses its ledger row and restores the loan balance. */
    @Transactional
    public EmployeeLoanResponse deleteRepayment(String loanId, String repaymentId) {
        EmployeeLoan loan = findLoan(loanId);
        EmployeeLoanRepayment repayment = loanRepaymentRepository.findById(repaymentId)
                .filter(r -> r.getLoan().getLoanId().equals(loanId))
                .orElseThrow(() -> new ResourceNotFoundException("LoanRepayment", "id", repaymentId));
        if (!EmployeeLoanRepayment.SOURCE_CASH.equals(repayment.getSource())) {
            throw new BadRequestException("Payslip deductions are undone by voiding the payslip");
        }
        if (!Boolean.TRUE.equals(loan.getIsActive())) {
            ensureNoOtherActiveLoan(loan);
        }
        MonetaryAssetTransaction ledgerRow = repayment.getTransaction();
        repayment.setTransaction(null);
        loanRepaymentRepository.delete(repayment);
        loanRepaymentRepository.flush();
        ledgerService.reverseIfActive(ledgerRow, "Loan repayment deleted");
        applyToLoanBalance(loan, repayment.getAmount());
        return toResponse(employeeLoanRepository.save(loan));
    }

    /** Deletes a loan that has no repayments, reversing its issue ledger row. */
    @Transactional
    public void deleteLoan(String loanId) {
        EmployeeLoan loan = findLoan(loanId);
        if (!loanRepaymentRepository.findByLoan_LoanIdOrderByRepaymentDateAscCreatedAtAsc(loanId).isEmpty()) {
            throw new BadRequestException("A loan with repayments cannot be deleted");
        }
        MonetaryAssetTransaction ledgerRow = loan.getTransaction();
        loan.setTransaction(null);
        employeeLoanRepository.delete(loan);
        employeeLoanRepository.flush();
        ledgerService.reverseIfActive(ledgerRow, "Loan deleted");
    }

    @Transactional
    public EmployeeLoanResponse updateLoan(String loanId, UpdateLoanRequest request) {
        EmployeeLoan loan = findLoan(loanId);
        if (request.getIsActive() != null) {
            if (request.getIsActive() && !Boolean.TRUE.equals(loan.getIsActive())) {
                ensureNoOtherActiveLoan(loan);
            }
            loan.setIsActive(request.getIsActive());
        }
        if (request.getCurrentBalance() != null) {
            loan.setCurrentBalance(request.getCurrentBalance());
        }
        if (request.getInstallment() != null) {
            loan.setInstallment(request.getInstallment());
        }
        return toResponse(employeeLoanRepository.save(loan));
    }

    public EmployeeLoanResponse getLoan(String loanId) {
        return toResponse(findLoan(loanId));
    }

    public List<EmployeeLoanResponse> getEmployeeLoans(String employeeId) {
        findEmployee(employeeId);
        return employeeLoanRepository.findByEmployee_EmployeeId(employeeId).stream().map(this::toResponse).toList();
    }

    public List<EmployeeLoanResponse> getActiveLoans() {
        return employeeLoanRepository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }

    /** Reduces (negative delta) or restores (positive delta) the balance and keeps is_active in step. */
    void applyToLoanBalance(EmployeeLoan loan, BigDecimal delta) {
        BigDecimal balance = loan.getCurrentBalance().add(delta).setScale(2, RoundingMode.HALF_UP);
        loan.setCurrentBalance(balance.max(BigDecimal.ZERO));
        loan.setIsActive(balance.signum() > 0);
    }

    void ensureNoOtherActiveLoan(EmployeeLoan loan) {
        boolean other = employeeLoanRepository.findByEmployee_EmployeeIdAndIsActiveTrue(loan.getEmployee().getEmployeeId())
                .stream().anyMatch(l -> !l.getLoanId().equals(loan.getLoanId()));
        if (other) {
            throw new BadRequestException("The employee has a newer active loan; settle it first");
        }
    }

    private EmployeeLoanResponse toResponse(EmployeeLoan loan) {
        return EmployeeLoanMapper.toResponse(loan,
                loanRepaymentRepository.findByLoan_LoanIdOrderByRepaymentDateAscCreatedAtAsc(loan.getLoanId()));
    }

    private EmployeeLoan findLoan(String loanId) {
        return employeeLoanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeLoan", "id", loanId));
    }

    // -- Reads ---------------------------------------------------------------

    public EmployeeTransactionResponse getEmployeeTransaction(String id) {
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeTransaction", "id", id)));
    }

    public PageResponse<EmployeeTransactionResponse> getAllEmployeeTransactions(Pageable pageable) {
        return PageResponse.from(employeeTransactionRepository.findAll(pageable)
                .map(EmployeeTransactionMapper::toResponse));
    }

    private Employee findEmployee(String id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
    }
}
