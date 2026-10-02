package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.expense.ExpenseMarkPaidRequest;
import com.madukotawatte.erp.dto.expense.ExpenseRequest;
import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.finance.ExpenseSummaryResponse;
import com.madukotawatte.erp.dto.finance.TrendPointResponse;
import com.madukotawatte.erp.entity.Expense;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.ExpenseMapper;
import com.madukotawatte.erp.repository.ExpenseRepository;
import com.madukotawatte.erp.service.support.TrendSupport;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    public static final String REF_EXPENSE = "EXPENSE";
    public static final String REF_LABOUR = "LABOUR";

    private final ExpenseRepository expenseRepository;
    private final LedgerService ledgerService;

    // ── CRUD ────────────────────────────────────────────────────────────────
    @Transactional
    public ExpenseResponse createExpense(ExpenseRequest request) {
        boolean isPaid = request.getIsPaid() == null || request.getIsPaid();

        Expense expense = new Expense();
        expense.setExpenseId(UUID.randomUUID().toString());
        expense.setType(request.getType());
        expense.setAmount(request.getAmount());
        expense.setTimestamp(request.getTimestamp());
        expense.setIsPaid(isPaid);
        expense.setStatus(isPaid ? "paid" : "pending");

        if (isPaid) {
            LocalDate paidOn = request.getPaymentDate() != null ? request.getPaymentDate() : request.getTimestamp().toLocalDate();
            pay(expense, request.getAccountCode(), paidOn);
        }
        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    public ExpenseResponse getExpense(String id) {
        return ExpenseMapper.toResponse(find(id));
    }

    public PageResponse<ExpenseResponse> getAllExpenses(String type, Boolean isPaid, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return PageResponse.from(expenseRepository.findAll(expenseSpec(type, isPaid, from, to), pageable).map(ExpenseMapper::toResponse));
    }

    /**
     * Edits an expense. If it was paid, its ledger row is reversed first and (when it stays paid) re-posted with
     * the new amount / account / date, so the ledger never keeps a stale payment.
     */
    @Transactional
    public ExpenseResponse updateExpense(String id, ExpenseRequest request) {
        Expense expense = find(id);
        assertNotSystemManaged(expense);

        boolean wasPaid = Boolean.TRUE.equals(expense.getIsPaid());
        boolean targetPaid = request.getIsPaid() != null ? request.getIsPaid() : wasPaid;
        MonetaryAssetTransaction oldTx = expense.getTransaction();
        boolean accountGiven = request.getAccountCode() != null && !request.getAccountCode().isBlank();
        String newAccount = accountGiven ? request.getAccountCode() : expense.getAccountCode();
        LocalDate newDate = request.getPaymentDate() != null ? request.getPaymentDate()
                : oldTx != null ? oldTx.getTransactionDate() : request.getTimestamp().toLocalDate();

        boolean legacyPaidWithoutLedger = wasPaid && targetPaid && oldTx == null && !accountGiven;
        boolean moneyUnchanged = legacyPaidWithoutLedger
                || (wasPaid && targetPaid && oldTx != null
                    && request.getAmount().compareTo(expense.getAmount()) == 0
                    && newAccount != null && newAccount.equals(oldTx.getAccountCode())
                    && newDate.equals(oldTx.getTransactionDate()));

        expense.setType(request.getType());
        expense.setTimestamp(request.getTimestamp());
        if (!moneyUnchanged) {
            if (oldTx != null) {
                ledgerService.reverseIfActive(oldTx, "Expense edited");
                expense.setTransaction(null);
            }
            expense.setAmount(request.getAmount());
            if (targetPaid) {
                pay(expense, newAccount, newDate);
            } else {
                expense.setAccountCode(null);
                expense.setIsPaid(false);
                expense.setStatus("pending");
            }
        } else {
            expense.setAmount(request.getAmount());
        }
        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(String id) {
        Expense expense = find(id);
        assertNotSystemManaged(expense);
        if (expense.getTransaction() != null) {
            ledgerService.reverseIfActive(expense.getTransaction(), "Expense deleted");
        }
        expenseRepository.delete(expense);
    }

    @Transactional
    public ExpenseResponse markExpensePaid(String id, ExpenseMarkPaidRequest request) {
        Expense expense = find(id);
        if (Boolean.TRUE.equals(expense.getIsPaid())) {
            throw new BadRequestException("Expense is already paid");
        }
        pay(expense, request.getAccountCode(), request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        return ExpenseMapper.toResponse(expenseRepository.save(expense));
    }

    private void pay(Expense expense, String accountCode, LocalDate paidOn) {
        if (accountCode == null || accountCode.isBlank()) {
            throw new BadRequestException("accountCode is required for a paid expense");
        }
        ledgerService.requireActiveAccount(accountCode);
        MonetaryAssetTransaction tx = ledgerService.post(accountCode, LedgerDirection.OUT, expense.getAmount(), paidOn,
                LedgerCategory.EXPENSE_PAYMENT, REF_EXPENSE, expense.getExpenseId(), expense.getType() + " expense");
        expense.setAccountCode(accountCode);
        expense.setTransaction(tx);
        expense.setIsPaid(true);
        expense.setStatus("paid");
    }

    private void assertNotSystemManaged(Expense expense) {
        if (expense.getReferenceType() != null) {
            throw new BadRequestException("This expense was created by " + expense.getReferenceType()
                    + " and must be changed from there");
        }
    }

    private Expense find(String id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", id));
    }

    // ── System-created expenses (labour, payroll, interest) ─────────────────

    /**
     * Records an already-paid expense owned by another business record: posts the ledger row with the given
     * category (e.g. LABOUR_PAYMENT) and links it to the referenced record.
     */
    @Transactional
    public Expense recordPaidSystemExpense(String type, BigDecimal amount, LocalDateTime timestamp, String accountCode,
                                           LedgerCategory ledgerCategory, String referenceType, String referenceId,
                                           String description) {
        Expense expense = new Expense();
        expense.setExpenseId(UUID.randomUUID().toString());
        expense.setType(type);
        expense.setAmount(amount);
        expense.setTimestamp(timestamp);
        expense.setReferenceType(referenceType);
        expense.setReferenceId(referenceId);
        MonetaryAssetTransaction tx = ledgerService.post(accountCode, LedgerDirection.OUT, amount, timestamp.toLocalDate(),
                ledgerCategory, referenceType, referenceId, description);
        expense.setAccountCode(accountCode);
        expense.setTransaction(tx);
        expense.setIsPaid(true);
        expense.setStatus("paid");
        return expenseRepository.save(expense);
    }

    /** Reverses the ledger rows and deletes the expenses owned by a business record. */
    @Transactional
    public void removeSystemExpenses(String referenceType, String referenceId, String reason) {
        for (Expense expense : expenseRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId)) {
            if (expense.getTransaction() != null) {
                ledgerService.reverseIfActive(expense.getTransaction(), reason);
            }
            expenseRepository.delete(expense);
        }
    }

    // ── Summaries ───────────────────────────────────────────────────────────
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
        TrendSupport.Window window = TrendSupport.resolveWindow(scale);
        List<Object[]> rows = expenseRepository
                .findAll(expenseSpec(null, null, window.from().atStartOfDay(), window.to().atTime(23, 59, 59)))
                .stream()
                .map(e -> new Object[]{e.getTimestamp().toLocalDate(), e.getAmount()})
                .collect(Collectors.toList());
        return TrendSupport.bucketSums(scale, rows);
    }

    private Specification<Expense> expenseSpec(String type, Boolean isPaid, LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── Payroll hooks (Wave 2a) ──

    /**
     * Records an already-paid system expense that is linked to a ledger row somebody else already posted
     * (payroll: the SALARY_PAYMENT row for the NET pay). Posts nothing itself, so the ledger is not double
     * counted. {@code ledgerRow} may be null when no money moved (net pay of zero).
     */
    @Transactional
    public Expense recordSystemExpenseLinkedToLedger(String type, BigDecimal amount, LocalDateTime timestamp,
                                                     String accountCode, MonetaryAssetTransaction ledgerRow,
                                                     String referenceType, String referenceId) {
        Expense expense = new Expense();
        expense.setExpenseId(UUID.randomUUID().toString());
        expense.setType(type);
        expense.setAmount(amount);
        expense.setTimestamp(timestamp);
        expense.setReferenceType(referenceType);
        expense.setReferenceId(referenceId);
        expense.setAccountCode(accountCode);
        expense.setTransaction(ledgerRow);
        expense.setIsPaid(true);
        expense.setStatus("paid");
        return expenseRepository.save(expense);
    }
}
