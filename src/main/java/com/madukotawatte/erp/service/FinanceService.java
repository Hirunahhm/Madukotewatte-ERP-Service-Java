package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanTransactionResponse;
import com.madukotawatte.erp.dto.estateloan.CreditCardLimitResponse;
import com.madukotawatte.erp.dto.estateloan.CreditCardStatementResponse;
import com.madukotawatte.erp.entity.CreditCardLimit;
import com.madukotawatte.erp.repository.CreditCardLimitRepository;
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
import org.springframework.data.domain.Sort;
import com.madukotawatte.erp.dto.estateloan.UpdateCreditCardLimitRequest;
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
    private final CreditCardLimitRepository creditCardLimitRepository;
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
        return getAssetBalances(null);
    }

    /** asOf == null returns the current balance; otherwise reconstructs the balance as of that date (inclusive). */
    public List<AssetBalanceResponse> getAssetBalances(LocalDate asOf) {
        if (asOf == null) {
            List<Object[]> results = monetaryAssetTransactionRepository.findBalanceByAssetType();
            return results.stream()
                    .map(row -> AssetBalanceResponse.builder()
                            .assetType((String) row[0])
                            .balance((BigDecimal) row[1])
                            .build())
                    .collect(Collectors.toList());
        }
        java.util.Map<String, BigDecimal> balances = new java.util.LinkedHashMap<>();
        for (Object[] row : monetaryAssetTransactionRepository.findAllOrderedByCreatedAt()) {
            String assetType = (String) row[0];
            java.time.LocalDateTime createdAt = (java.time.LocalDateTime) row[1];
            BigDecimal newAmount = (BigDecimal) row[2];
            if (createdAt.toLocalDate().isAfter(asOf)) {
                continue;
            }
            balances.put(assetType, newAmount);
        }
        return balances.entrySet().stream()
                .map(e -> AssetBalanceResponse.builder().assetType(e.getKey()).balance(e.getValue()).build())
                .collect(Collectors.toList());
    }

    @Transactional
    public MonetaryAssetTransactionResponse createMonetaryTransaction(MonetaryAssetTransactionRequest request) {
        MonetaryAssetTransaction transaction = createMonetaryTransactionEntity(
                request.getTransactionType(), request.getAssetType(), request.getAmount());
        return MonetaryAssetMapper.toResponse(transaction);
    }

    private MonetaryAssetTransaction createMonetaryTransactionEntity(String transactionType, String assetType, BigDecimal amount) {
        List<MonetaryAssetTransaction> existing = monetaryAssetTransactionRepository.findByAssetTypeOrderByCreatedAtAsc(assetType);
        BigDecimal lastAmount = BigDecimal.ZERO;
        if (!existing.isEmpty()) {
            lastAmount = existing.get(existing.size() - 1).getNewAmount();
        }

        BigDecimal newAmount;
        if ("money in".equals(transactionType)) {
            newAmount = lastAmount.add(amount);
        } else if ("money out".equals(transactionType)) {
            newAmount = lastAmount.subtract(amount);
        } else {
            throw new BadRequestException("Invalid transaction type: " + transactionType);
        }

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + assetType);
        }

        MonetaryAssetTransaction transaction = new MonetaryAssetTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setTransactionType(transactionType);
        transaction.setAssetType(assetType);
        transaction.setLastAmount(lastAmount);
        transaction.setNewAmount(newAmount);

        return monetaryAssetTransactionRepository.save(transaction);
    }

    // Labour's payment_type column is constrained to the coarse ('cash' | 'bank_transfer')
    // domain, unlike Expenses' bank-specific one — map to a concrete ledger asset so a
    // labour payout still settles against a real account.
    private static final Map<String, String> LABOUR_PAYMENT_TO_LEDGER_ASSET = Map.of(
            "cash", "Cash",
            "bank_transfer", "Bank-BOC"
    );
    private static final Map<String, String> LABOUR_PAYMENT_TO_EXPENSE_PAYMENT_TYPE = Map.of(
            "cash", "Cash",
            "bank_transfer", "Bank Transfer-BOC"
    );

    /**
     * Labour payouts must settle through the same Cash & Debt ledger and Financials
     * expense tracking as any other outgoing payment — otherwise a "paid" labour record
     * silently leaves both unaware that money actually left the estate's accounts.
     */
    @Transactional
    public void recordLabourExpense(BigDecimal amount, String labourPaymentType, java.time.LocalDateTime timestamp) {
        String ledgerAsset = LABOUR_PAYMENT_TO_LEDGER_ASSET.getOrDefault(labourPaymentType, "Cash");
        String expensePaymentType = LABOUR_PAYMENT_TO_EXPENSE_PAYMENT_TYPE.getOrDefault(labourPaymentType, "Cash");

        MonetaryAssetTransaction ledgerTx = createMonetaryTransactionEntity("money out", ledgerAsset, amount);

        Expense expense = new Expense();
        expense.setExpenseId(UUID.randomUUID().toString());
        expense.setType("Labor");
        expense.setAmount(amount);
        expense.setTimestamp(timestamp);
        expense.setIsPaid(true);
        expense.setStatus("paid");
        expense.setPaymentType(expensePaymentType);
        expense.setTransaction(ledgerTx);
        expenseRepository.save(expense);
    }

    public PageResponse<MonetaryAssetTransactionResponse> getAllMonetaryTransactions(
            String assetType, String transactionType, LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.from(monetaryAssetTransactionRepository
                .findAll(monetarySpec(assetType, transactionType, from, to), pageable)
                .map(MonetaryAssetMapper::toResponse));
    }

    public MonetaryAssetTransactionResponse getMonetaryTransaction(String id) {
        MonetaryAssetTransaction transaction = monetaryAssetTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MonetaryAssetTransaction", "id", id));
        return MonetaryAssetMapper.toResponse(transaction);
    }

    private Specification<MonetaryAssetTransaction> monetarySpec(String assetType, String transactionType, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (assetType != null && !assetType.isBlank()) {
                predicates.add(cb.equal(root.get("assetType"), assetType));
            }
            if (transactionType != null && !transactionType.isBlank()) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    // ── Estate Loans ────────────────────────────────────────────
    public List<EstateLoanBalanceResponse> getEstateLoanBalances() {
        return getEstateLoanBalances(null);
    }

    /** asOf == null returns the current balance; otherwise reconstructs the balance as of that date (inclusive). */
    public List<EstateLoanBalanceResponse> getEstateLoanBalances(LocalDate asOf) {
        List<String> loanTypes = List.of("credit-card - Peoples", "credit-card - Sampath", "Loan-mom", "Loan-other");
        return loanTypes.stream()
                .map(loanType -> {
                    List<EstateLoanTransaction> transactions = estateLoanTransactionRepository.findByLoanTypeOrderByCreatedAtAsc(loanType);
                    BigDecimal balance = BigDecimal.ZERO;
                    for (EstateLoanTransaction t : transactions) {
                        if (asOf != null && t.getCreatedAt().toLocalDate().isAfter(asOf)) {
                            break;
                        }
                        balance = t.getNewAmount();
                    }
                    return EstateLoanBalanceResponse.builder()
                            .loanType(loanType)
                            .balance(balance)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public EstateLoanTransactionResponse createEstateLoanTransaction(EstateLoanTransactionRequest request) {
        List<EstateLoanTransaction> existing = estateLoanTransactionRepository.findByLoanTypeOrderByCreatedAtAsc(request.getLoanType());
        BigDecimal lastAmount = BigDecimal.ZERO;
        if (!existing.isEmpty()) {
            lastAmount = existing.get(existing.size() - 1).getNewAmount();
        }

        BigDecimal newAmount;
        if ("borrow".equals(request.getTransactionType())) {
            newAmount = lastAmount.add(request.getAmount());
        } else if ("repay".equals(request.getTransactionType())) {
            newAmount = lastAmount.subtract(request.getAmount());
        } else {
            throw new BadRequestException("Invalid transaction type: " + request.getTransactionType());
        }

        if (newAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Repayment exceeds outstanding balance on " + request.getLoanType());
        }

        EstateLoanTransaction transaction = new EstateLoanTransaction();
        transaction.setId(UUID.randomUUID().toString());
        transaction.setLoanType(request.getLoanType());
        transaction.setTransactionType(request.getTransactionType());
        transaction.setLastAmount(lastAmount);
        transaction.setNewAmount(newAmount);

        return EstateLoanMapper.toResponse(estateLoanTransactionRepository.save(transaction));
    }

    public PageResponse<EstateLoanTransactionResponse> getAllEstateLoanTransactions(
            String loanType, String transactionType, LocalDate from, LocalDate to, Pageable pageable) {
        return PageResponse.from(estateLoanTransactionRepository
                .findAll(loanSpec(loanType, transactionType, from, to), pageable)
                .map(EstateLoanMapper::toResponse));
    }

    public EstateLoanTransactionResponse getEstateLoanTransaction(String id) {
        EstateLoanTransaction transaction = estateLoanTransactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EstateLoanTransaction", "id", id));
        return EstateLoanMapper.toResponse(transaction);
    }

    private Specification<EstateLoanTransaction> loanSpec(String loanType, String transactionType, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (loanType != null && !loanType.isBlank()) {
                predicates.add(cb.equal(root.get("loanType"), loanType));
            }
            if (transactionType != null && !transactionType.isBlank()) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private static final List<String> CREDIT_CARD_TYPES = List.of("credit-card - Peoples", "credit-card - Sampath");

    public CreditCardStatementResponse getCreditCardStatement(String loanType, LocalDate from, LocalDate to) {
        if (!CREDIT_CARD_TYPES.contains(loanType)) {
            throw new BadRequestException("Not a credit card loan type: " + loanType);
        }
        List<EstateLoanTransaction> inRange = estateLoanTransactionRepository.findAll(
                loanSpec(loanType, null, from, to),
                Sort.by(Sort.Direction.ASC, "createdAt"));

        BigDecimal openingBalance = BigDecimal.ZERO;
        java.time.LocalDateTime fromStart = from.atStartOfDay();
        for (Object[] row : estateLoanTransactionRepository.findOrderedByCreatedAtForLoanType(loanType)) {
            java.time.LocalDateTime createdAt = (java.time.LocalDateTime) row[1];
            if (createdAt.isBefore(fromStart)) {
                openingBalance = (BigDecimal) row[2];
            } else {
                break;
            }
        }

        BigDecimal totalCharges = BigDecimal.ZERO;
        BigDecimal totalPayments = BigDecimal.ZERO;
        for (EstateLoanTransaction tx : inRange) {
            BigDecimal delta = tx.getNewAmount().subtract(tx.getLastAmount()).abs();
            if ("borrow".equals(tx.getTransactionType())) {
                totalCharges = totalCharges.add(delta);
            } else if ("repay".equals(tx.getTransactionType())) {
                totalPayments = totalPayments.add(delta);
            }
        }

        return CreditCardStatementResponse.builder()
                .loanType(loanType)
                .from(from)
                .to(to)
                .openingBalance(openingBalance)
                .totalCharges(totalCharges)
                .totalPayments(totalPayments)
                .closingBalance(openingBalance.add(totalCharges).subtract(totalPayments))
                .transactions(inRange.stream().map(EstateLoanMapper::toResponse).collect(Collectors.toList()))
                .build();
    }

    public List<CreditCardLimitResponse> getCreditCardLimits() {
        Map<String, BigDecimal> balances = getEstateLoanBalances().stream()
                .collect(Collectors.toMap(EstateLoanBalanceResponse::getLoanType, EstateLoanBalanceResponse::getBalance));
        return CREDIT_CARD_TYPES.stream()
                .map(type -> {
                    BigDecimal limit = creditCardLimitRepository.findById(type)
                            .map(CreditCardLimit::getCreditLimit)
                            .orElse(BigDecimal.ZERO);
                    BigDecimal balance = balances.getOrDefault(type, BigDecimal.ZERO);
                    return CreditCardLimitResponse.builder()
                            .loanType(type)
                            .creditLimit(limit)
                            .balance(balance)
                            .availableCredit(limit.subtract(balance))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CreditCardLimitResponse updateCreditCardLimit(UpdateCreditCardLimitRequest request) {
        if (!CREDIT_CARD_TYPES.contains(request.getLoanType())) {
            throw new BadRequestException("Not a credit card loan type: " + request.getLoanType());
        }
        CreditCardLimit limit = creditCardLimitRepository.findById(request.getLoanType())
                .orElse(CreditCardLimit.builder().loanType(request.getLoanType()).build());
        limit.setCreditLimit(request.getCreditLimit());
        creditCardLimitRepository.save(limit);

        BigDecimal balance = getEstateLoanBalances().stream()
                .filter(b -> b.getLoanType().equals(request.getLoanType()))
                .map(EstateLoanBalanceResponse::getBalance)
                .findFirst().orElse(BigDecimal.ZERO);

        return CreditCardLimitResponse.builder()
                .loanType(request.getLoanType())
                .creditLimit(request.getCreditLimit())
                .balance(balance)
                .availableCredit(request.getCreditLimit().subtract(balance))
                .build();
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

    public SalesSummaryResponse getSalesSummary(LocalDate from, LocalDate to) {
        Object[] row = salesLedgerRepository.sumByPaidStatus(from, to);
        BigDecimal received = (BigDecimal) row[0];
        BigDecimal pending = (BigDecimal) row[1];
        return SalesSummaryResponse.builder()
                .totalSales(received.add(pending))
                .received(received)
                .pending(pending)
                .build();
    }

    public List<CategoryTotalResponse> getSalesDistribution(LocalDate from, LocalDate to) {
        return salesLedgerRepository.sumByCategory(from, to).stream()
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

    // Running-balance trend: rows are (type, createdAt, newAmount) across ALL time (not just the window),
    // ordered by createdAt ascending, so the balance carried into the window's first bucket is correct.
    private List<TrendPointResponse> bucketBalanceTrend(String scale, TrendWindow window, List<Object[]> orderedRows) {
        List<String> labels = switch (scale) {
            case "week" -> List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
            case "year" -> List.of("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
            default -> List.of("W1", "W2", "W3", "W4");
        };
        List<LocalDate> boundaries = bucketEndDates(scale, window);

        Map<String, BigDecimal> currentBalance = new java.util.HashMap<>();
        int idx = 0;
        List<TrendPointResponse> points = new java.util.ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            java.time.LocalDateTime cutoff = boundaries.get(i).atTime(23, 59, 59);
            while (idx < orderedRows.size()) {
                Object[] row = orderedRows.get(idx);
                java.time.LocalDateTime createdAt = (java.time.LocalDateTime) row[1];
                if (createdAt.isAfter(cutoff)) break;
                currentBalance.put((String) row[0], (BigDecimal) row[2]);
                idx++;
            }
            BigDecimal total = currentBalance.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            points.add(TrendPointResponse.builder().name(labels.get(i)).total(total).build());
        }
        return points;
    }

    private List<LocalDate> bucketEndDates(String scale, TrendWindow window) {
        return switch (scale) {
            case "week" -> java.util.stream.IntStream.range(0, 7)
                    .mapToObj(window.from()::plusDays)
                    .collect(Collectors.toList());
            case "year" -> java.util.stream.IntStream.rangeClosed(1, 12)
                    .mapToObj(m -> LocalDate.of(window.from().getYear(), m, 1).plusMonths(1).minusDays(1))
                    .collect(Collectors.toList());
            default -> List.of(
                    window.from().plusDays(6),
                    window.from().plusDays(13),
                    window.from().plusDays(20),
                    window.to());
        };
    }

    public List<TrendPointResponse> getAssetBalanceTrend(String scale) {
        TrendWindow window = resolveTrendWindow(scale);
        return bucketBalanceTrend(scale, window, monetaryAssetTransactionRepository.findAllOrderedByCreatedAt());
    }

    public List<TrendPointResponse> getLoanBalanceTrend(String scale) {
        TrendWindow window = resolveTrendWindow(scale);
        return bucketBalanceTrend(scale, window, estateLoanTransactionRepository.findAllOrderedByCreatedAt());
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

    public ExpenseSummaryResponse getExpenseSummary(LocalDate from, LocalDate to) {
        List<Expense> all = findExpensesInRange(from, to);
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

    public List<CategoryTotalResponse> getExpenseDistribution(LocalDate from, LocalDate to) {
        return findExpensesInRange(from, to).stream()
                .collect(Collectors.groupingBy(Expense::getType, Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .map(e -> CategoryTotalResponse.builder().category(e.getKey()).total(e.getValue()).build())
                .collect(Collectors.toList());
    }

    private List<Expense> findExpensesInRange(LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return expenseRepository.findAll();
        }
        return expenseRepository.findAll(expenseSpec(null, null,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.atTime(23, 59, 59) : null));
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
