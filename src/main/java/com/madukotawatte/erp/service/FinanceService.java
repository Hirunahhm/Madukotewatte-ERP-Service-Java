package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionResponse;
import com.madukotawatte.erp.dto.expense.ExpenseMarkPaidRequest;
import com.madukotawatte.erp.dto.expense.ExpenseRequest;
import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.finance.ExpenseSummaryResponse;
import com.madukotawatte.erp.dto.finance.TrendPointResponse;
import com.madukotawatte.erp.dto.monetary.AssetBalanceResponse;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionRequest;
import com.madukotawatte.erp.dto.monetary.MonetaryAssetTransactionResponse;
import com.madukotawatte.erp.dto.sales.SalesBananaRequest;
import com.madukotawatte.erp.dto.sales.SalesBananaResponse;
import com.madukotawatte.erp.dto.sales.SalesCoconutRequest;
import com.madukotawatte.erp.dto.sales.SalesCoconutResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexRequest;
import com.madukotawatte.erp.dto.sales.SalesLatexResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexUpdateRequest;
import com.madukotawatte.erp.dto.sales.SalesLedgerRowResponse;
import com.madukotawatte.erp.dto.sales.SalesManiocRequest;
import com.madukotawatte.erp.dto.sales.SalesManiocResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidResponse;
import com.madukotawatte.erp.dto.sales.SalesSummaryResponse;
import com.madukotawatte.erp.entity.*;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.*;
import com.madukotawatte.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinanceService {

    private final MonetaryAssetTransactionRepository monetaryAssetTransactionRepository;
    private final EstateLoanTransactionRepository estateLoanTransactionRepository;
    private final SalesLatexRepository salesLatexRepository;
    private final SalesRubberSolidRepository salesRubberSolidRepository;
    private final SalesManiocRepository salesManiocRepository;
    private final SalesCoconutRepository salesCoconutRepository;
    private final SalesBananaRepository salesBananaRepository;
    private final SalesLedgerRepository salesLedgerRepository;
    private final LoadRepository loadRepository;
    private final ExpenseRepository expenseRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final EmployeeRepository employeeRepository;

    // ── Monetary Assets ─────────────────────────────────────────
    public List<AssetBalanceResponse> getAssetBalances() {
        List<Object[]> results = monetaryAssetTransactionRepository.findBalanceByAssetType();
        return results.stream()
                .map(row -> AssetBalanceResponse.builder()
                        .assetType((String) row[0])
                        .balance((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public MonetaryAssetTransactionResponse createMonetaryTransaction(MonetaryAssetTransactionRequest request) {
        List<MonetaryAssetTransaction> existing = monetaryAssetTransactionRepository.findByAssetType(request.getAssetType());
        BigDecimal lastAmount = BigDecimal.ZERO;
        if (!existing.isEmpty()) {
            lastAmount = existing.get(existing.size() - 1).getNewAmount();
        }

        BigDecimal newAmount;
        if ("money in".equals(request.getTransactionType())) {
            newAmount = lastAmount.add(request.getAmount());
        } else if ("money out".equals(request.getTransactionType())) {
            newAmount = lastAmount.subtract(request.getAmount());
        } else {
            throw new BadRequestException("Invalid transaction type: " + request.getTransactionType());
        }

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + request.getAssetType());
        }

        MonetaryAssetTransaction transaction = new MonetaryAssetTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setAssetType(request.getAssetType());
        transaction.setLastAmount(lastAmount);
        transaction.setNewAmount(newAmount);

        return MonetaryAssetMapper.toResponse(monetaryAssetTransactionRepository.save(transaction));
    }

    public PageResponse<MonetaryAssetTransactionResponse> getAllMonetaryTransactions(Pageable pageable) {
        return PageResponse.from(monetaryAssetTransactionRepository.findAll(pageable)
                .map(MonetaryAssetMapper::toResponse));
    }

    public MonetaryAssetTransactionResponse getMonetaryTransaction(String id) {
        MonetaryAssetTransaction transaction = monetaryAssetTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", id));
        return MonetaryAssetMapper.toResponse(transaction);
    }

    // ── Estate Loans ────────────────────────────────────────────
    public List<EstateLoanBalanceResponse> getEstateLoanBalances() {
        List<String> loanTypes = List.of("credit-card - Peoples", "credit-card - Sampath", "Loan-mom", "Loan-other");
        return loanTypes.stream()
                .map(loanType -> {
                    List<EstateLoanTransaction> transactions = estateLoanTransactionRepository.findByLoanType(loanType);
                    BigDecimal balance = transactions.isEmpty() ? BigDecimal.ZERO
                            : transactions.get(transactions.size() - 1).getNewAmount();
                    return EstateLoanBalanceResponse.builder()
                            .loanType(loanType)
                            .balance(balance)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public EstateLoanTransactionResponse createEstateLoanTransaction(EstateLoanTransactionRequest request) {
        List<EstateLoanTransaction> existing = estateLoanTransactionRepository.findByLoanType(request.getLoanType());
        BigDecimal lastAmount = BigDecimal.ZERO;
        if (!existing.isEmpty()) {
            lastAmount = existing.get(existing.size() - 1).getNewAmount();
        }
        BigDecimal newAmount = lastAmount.add(request.getAmount());

        EstateLoanTransaction transaction = new EstateLoanTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setLoanType(request.getLoanType());
        transaction.setLastAmount(lastAmount);
        transaction.setNewAmount(newAmount);

        return EstateLoanMapper.toResponse(estateLoanTransactionRepository.save(transaction));
    }

    public PageResponse<EstateLoanTransactionResponse> getAllEstateLoanTransactions(Pageable pageable) {
        return PageResponse.from(estateLoanTransactionRepository.findAll(pageable)
                .map(EstateLoanMapper::toResponse));
    }

    public EstateLoanTransactionResponse getEstateLoanTransaction(String id) {
        EstateLoanTransaction transaction = estateLoanTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EstateLoanTransaction", "id", id));
        return EstateLoanMapper.toResponse(transaction);
    }

    // ── Sales Latex ─────────────────────────────────────────────
    @Transactional
    public SalesLatexResponse createSalesLatex(SalesLatexRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", request.getLoadId()));
        SalesLatex sale = SalesLatexMapper.toEntity(request, load);
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    public SalesLatexResponse getSalesLatex(String id) {
        SalesLatex sale = salesLatexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id));
        return SalesLatexMapper.toResponse(sale);
    }

    public PageResponse<SalesLatexResponse> getAllSalesLatex(Pageable pageable) {
        return PageResponse.from(salesLatexRepository.findAll(pageable).map(SalesLatexMapper::toResponse));
    }

    public List<SalesLatexResponse> getUnpaidSales() {
        return salesLatexRepository.findByIsPaymentReceivedFalse().stream()
                .map(SalesLatexMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalesLatexResponse markPaymentReceived(String id, SalesLatexUpdateRequest request) {
        SalesLatex sale = salesLatexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id));
        sale.setIsPaymentReceived(true);
        if (request.getMonetaryTransactionId() != null) {
            MonetaryAssetTransaction transaction = monetaryAssetTransactionRepository
                    .findById(request.getMonetaryTransactionId())
                    .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", request.getMonetaryTransactionId()));
            sale.setTransaction(transaction);
        }
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    // ── Sales Rubber Solid ───────────────────────────────────────
    @Transactional
    public SalesRubberSolidResponse createSalesRubberSolid(SalesRubberSolidRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", request.getLoadId()));
        SalesRubberSolid sale = SalesRubberSolidMapper.toEntity(request, load);
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.save(sale));
    }

    public SalesRubberSolidResponse getSalesRubberSolid(String id) {
        SalesRubberSolid sale = salesRubberSolidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesRubberSolid", "id", id));
        return SalesRubberSolidMapper.toResponse(sale);
    }

    public PageResponse<SalesRubberSolidResponse> getAllSalesRubberSolid(Pageable pageable) {
        return PageResponse.from(salesRubberSolidRepository.findAll(pageable).map(SalesRubberSolidMapper::toResponse));
    }

    @Transactional
    public SalesRubberSolidResponse markSalesRubberSolidPaid(String id, SalesMarkPaidRequest request) {
        SalesRubberSolid sale = salesRubberSolidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesRubberSolid", "id", id));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setPaymentType(request.getPaymentType());
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.save(sale));
    }

    // ── Sales Manioc ─────────────────────────────────────────────
    @Transactional
    public SalesManiocResponse createSalesManioc(SalesManiocRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", request.getLoadId()));
        SalesManioc sale = SalesManiocMapper.toEntity(request, load);
        return SalesManiocMapper.toResponse(salesManiocRepository.save(sale));
    }

    public SalesManiocResponse getSalesManioc(String id) {
        SalesManioc sale = salesManiocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesManioc", "id", id));
        return SalesManiocMapper.toResponse(sale);
    }

    public PageResponse<SalesManiocResponse> getAllSalesManioc(Pageable pageable) {
        return PageResponse.from(salesManiocRepository.findAll(pageable).map(SalesManiocMapper::toResponse));
    }

    @Transactional
    public SalesManiocResponse markSalesManiocPaid(String id, SalesMarkPaidRequest request) {
        SalesManioc sale = salesManiocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesManioc", "id", id));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setPaymentType(request.getPaymentType());
        return SalesManiocMapper.toResponse(salesManiocRepository.save(sale));
    }

    // ── Sales Coconut ────────────────────────────────────────────
    @Transactional
    public SalesCoconutResponse createSalesCoconut(SalesCoconutRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", request.getLoadId()));
        SalesCoconut sale = SalesCoconutMapper.toEntity(request, load);
        return SalesCoconutMapper.toResponse(salesCoconutRepository.save(sale));
    }

    public SalesCoconutResponse getSalesCoconut(String id) {
        SalesCoconut sale = salesCoconutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesCoconut", "id", id));
        return SalesCoconutMapper.toResponse(sale);
    }

    public PageResponse<SalesCoconutResponse> getAllSalesCoconut(Pageable pageable) {
        return PageResponse.from(salesCoconutRepository.findAll(pageable).map(SalesCoconutMapper::toResponse));
    }

    @Transactional
    public SalesCoconutResponse markSalesCoconutPaid(String id, SalesMarkPaidRequest request) {
        SalesCoconut sale = salesCoconutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesCoconut", "id", id));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setPaymentType(request.getPaymentType());
        return SalesCoconutMapper.toResponse(salesCoconutRepository.save(sale));
    }

    // ── Sales Banana ─────────────────────────────────────────────
    @Transactional
    public SalesBananaResponse createSalesBanana(SalesBananaRequest request) {
        Load load = loadRepository.findById(request.getLoadId())
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", request.getLoadId()));
        SalesBanana sale = SalesBananaMapper.toEntity(request, load);
        return SalesBananaMapper.toResponse(salesBananaRepository.save(sale));
    }

    public SalesBananaResponse getSalesBanana(String id) {
        SalesBanana sale = salesBananaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesBanana", "id", id));
        return SalesBananaMapper.toResponse(sale);
    }

    public PageResponse<SalesBananaResponse> getAllSalesBanana(Pageable pageable) {
        return PageResponse.from(salesBananaRepository.findAll(pageable).map(SalesBananaMapper::toResponse));
    }

    @Transactional
    public SalesBananaResponse markSalesBananaPaid(String id, SalesMarkPaidRequest request) {
        SalesBanana sale = salesBananaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesBanana", "id", id));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setPaymentType(request.getPaymentType());
        return SalesBananaMapper.toResponse(salesBananaRepository.save(sale));
    }

    // ── Sales Ledger + Stats (cross-category) ───────────────────
    public PageResponse<SalesLedgerRowResponse> getSalesLedger(String category, String status, LocalDate from, LocalDate to, Pageable pageable) {
        List<Object[]> rows = salesLedgerRepository.findLedgerRows(category, status, from, to, pageable.getPageSize(), (int) pageable.getOffset());
        long total = salesLedgerRepository.countLedgerRows(category, status, from, to);
        List<SalesLedgerRowResponse> content = rows.stream()
                .map(r -> SalesLedgerRowResponse.builder()
                        .saleId((String) r[0])
                        .category((String) r[1])
                        .loadId((String) r[2])
                        .saleDate(toLocalDate(r[3]))
                        .amount((BigDecimal) r[4])
                        .status((String) r[5])
                        .paymentType((String) r[6])
                        .build())
                .collect(Collectors.toList());
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return PageResponse.<SalesLedgerRowResponse>builder()
                .content(content)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(total)
                .totalPages(totalPages)
                .last(pageable.getPageNumber() >= totalPages - 1)
                .build();
    }

    public SalesSummaryResponse getSalesSummary() {
        Object[] row = salesLedgerRepository.sumByPaidStatus();
        BigDecimal received = (BigDecimal) row[0];
        BigDecimal pending = (BigDecimal) row[1];
        return SalesSummaryResponse.builder()
                .totalSales(received.add(pending))
                .received(received)
                .pending(pending)
                .build();
    }

    public List<CategoryTotalResponse> getSalesDistribution() {
        return salesLedgerRepository.sumByCategory().stream()
                .map(r -> CategoryTotalResponse.builder().category((String) r[0]).total((BigDecimal) r[1]).build())
                .collect(Collectors.toList());
    }

    public List<TrendPointResponse> getSalesTrend(String scale) {
        TrendWindow window = resolveTrendWindow(scale);
        List<Object[]> rows = salesLedgerRepository.findAmountsInRange(window.from(), window.to());
        return bucketTrend(scale, window, rows);
    }

    private record TrendWindow(LocalDate from, LocalDate to) {}

    private TrendWindow resolveTrendWindow(String scale) {
        LocalDate today = LocalDate.now();
        return switch (scale) {
            case "week" -> new TrendWindow(today.with(DayOfWeek.MONDAY), today.with(DayOfWeek.MONDAY).plusDays(6));
            case "year" -> new TrendWindow(today.withDayOfYear(1), today.withMonth(12).withDayOfMonth(31));
            default -> new TrendWindow(today.withDayOfMonth(1), today.withDayOfMonth(1).plusMonths(1).minusDays(1));
        };
    }

    private List<TrendPointResponse> bucketTrend(String scale, TrendWindow window, List<Object[]> rows) {
        List<String> labels = switch (scale) {
            case "week" -> List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
            case "year" -> List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
            default -> List.of("W1", "W2", "W3", "W4");
        };
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String label : labels) totals.put(label, BigDecimal.ZERO);

        for (Object[] row : rows) {
            LocalDate date = toLocalDate(row[0]);
            BigDecimal amount = (BigDecimal) row[1];
            String label = bucketLabel(scale, date);
            totals.merge(label, amount, BigDecimal::add);
        }

        return totals.entrySet().stream()
                .map(e -> TrendPointResponse.builder().name(e.getKey()).total(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private String bucketLabel(String scale, LocalDate date) {
        return switch (scale) {
            case "week" -> date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            case "year" -> date.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            default -> "W" + Math.min(4, ((date.getDayOfMonth() - 1) / 7) + 1);
        };
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate();
        throw new IllegalStateException("Unexpected date type: " + value.getClass());
    }

    // ── Expenses ────────────────────────────────────────────────
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        boolean isPaid = request.getIsPaid() == null || request.getIsPaid();

        Expense expense = new Expense();
        expense.setExpenseId(UUID.randomUUID().toString());
        expense.setType(request.getType());
        expense.setAmount(request.getAmount());
        expense.setTimestamp(request.getTimestamp());
        expense.setIsPaid(isPaid);

        if (isPaid) {
            linkExpensePayment(expense, request.getPaymentType(), request.getMonetaryTransactionId(), request.getEstateLoanTransactionId());
            expense.setStatus("paid");
        } else {
            expense.setStatus("pending");
        }

        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    public ExpenseResponse getExpense(String id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        return ExpenseMapper.toResponse(expense);
    }

    public PageResponse<ExpenseResponse> getAllExpenses(String type, Boolean isPaid, java.time.LocalDateTime from, java.time.LocalDateTime to, Pageable pageable) {
        return PageResponse.from(expenseRepository.findAll(expenseSpec(type, isPaid, from, to), pageable).map(ExpenseMapper::toResponse));
    }

    @Transactional
    public ExpenseResponse updateExpense(String id, ExpenseRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        expense.setType(request.getType());
        expense.setAmount(request.getAmount());
        expense.setTimestamp(request.getTimestamp());
        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(String id) {
        if (!expenseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Expense", "id", id);
        }
        expenseRepository.deleteById(id);
    }

    @Transactional
    public ExpenseResponse markExpensePaid(String id, ExpenseMarkPaidRequest request) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
        linkExpensePayment(expense, request.getPaymentType(), request.getMonetaryTransactionId(), request.getEstateLoanTransactionId());
        expense.setIsPaid(true);
        expense.setStatus("paid");
        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    private void linkExpensePayment(Expense expense, String paymentType, String monetaryTransactionId, String estateLoanTransactionId) {
        boolean hasMonetary = monetaryTransactionId != null;
        boolean hasLoan = estateLoanTransactionId != null;

        if (hasMonetary == hasLoan) {
            throw new BadRequestException("Exactly one of monetaryTransactionId or estateLoanTransactionId must be provided");
        }

        expense.setPaymentType(paymentType);
        if (hasMonetary) {
            MonetaryAssetTransaction monetaryTx = monetaryAssetTransactionRepository
                    .findById(monetaryTransactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", monetaryTransactionId));
            expense.setTransaction(monetaryTx);
        } else {
            EstateLoanTransaction loanTx = estateLoanTransactionRepository
                    .findById(estateLoanTransactionId)
                    .orElseThrow(() -> new ResourceNotFoundException("EstateLoanTransaction", "id", estateLoanTransactionId));
            expense.setEstateLoanTransaction(loanTx);
        }
    }

    public ExpenseSummaryResponse getExpenseSummary() {
        List<Expense> all = expenseRepository.findAll();
        BigDecimal paid = all.stream().filter(Expense::getIsPaid).map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = all.stream().filter(e -> !e.getIsPaid()).map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ExpenseSummaryResponse.builder()
                .totalExpenses(paid.add(pending))
                .paid(paid)
                .pending(pending)
                .build();
    }

    public List<CategoryTotalResponse> getExpenseDistribution() {
        return expenseRepository.findAll().stream()
                .collect(Collectors.groupingBy(Expense::getType, Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> CategoryTotalResponse.builder().category(e.getKey()).total(e.getValue()).build())
                .collect(Collectors.toList());
    }

    public List<TrendPointResponse> getExpenseTrend(String scale) {
        TrendWindow window = resolveTrendWindow(scale);
        List<Object[]> rows = expenseRepository
                .findAll(expenseSpec(null, null,
                        window.from().atStartOfDay(), window.to().atTime(23, 59, 59)))
                .stream()
                .map(e -> new Object[]{e.getTimestamp().toLocalDate(), e.getAmount()})
                .collect(Collectors.toList());
        return bucketTrend(scale, window, rows);
    }

    private Specification<Expense> expenseSpec(String type, Boolean isPaid, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (isPaid != null) {
                predicates.add(cb.equal(root.get("isPaid"), isPaid));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), to));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ── Employee Transactions ───────────────────────────────────
    @Transactional
    public EmployeeTransactionResponse createEmployeeTransaction(EmployeeTransactionRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", request.getEmployeeId()));
        com.madukotawatte.erp.entity.EmployeeTransaction transaction = EmployeeTransactionMapper.toEntity(request, employee);
        return EmployeeTransactionMapper.toResponse(employeeTransactionRepository.save(transaction));
    }

    public EmployeeTransactionResponse getEmployeeTransaction(String id) {
        com.madukotawatte.erp.entity.EmployeeTransaction transaction = employeeTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EmployeeTransaction", "id", id));
        return EmployeeTransactionMapper.toResponse(transaction);
    }

    public PageResponse<EmployeeTransactionResponse> getAllEmployeeTransactions(Pageable pageable) {
        return PageResponse.from(employeeTransactionRepository.findAll(pageable)
                .map(EmployeeTransactionMapper::toResponse));
    }
}
