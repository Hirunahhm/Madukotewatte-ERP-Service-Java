package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.estateloan.BorrowingRowResponse;
import com.madukotawatte.erp.dto.estateloan.BorrowingScheduleResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanDrawdownRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanMovementResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanPaymentRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanRequest;
import com.madukotawatte.erp.dto.estateloan.EstateLoanResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanUpdateRequest;
import com.madukotawatte.erp.dto.estateloan.LoanBalanceAsOfResponse;
import com.madukotawatte.erp.entity.EstateLoan;
import com.madukotawatte.erp.entity.EstateLoanTransaction;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.EstateLoanMapper;
import com.madukotawatte.erp.repository.EstateLoanRepository;
import com.madukotawatte.erp.repository.EstateLoanTransactionRepository;
import com.madukotawatte.erp.repository.MonetaryAssetTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Named estate loans. Every money movement is an estate_loan_transactions row (type borrow or repay) dated by
 * transaction_date, linked to its principal ledger row. Interest paid with a repayment is an "Interest" system
 * expense (reference ESTATE_LOAN_INTEREST / movement id) posted through the expense hook.
 */
@Service
@RequiredArgsConstructor
public class EstateLoanService {

    public static final String REF_ESTATE_LOAN = "ESTATE_LOAN";
    public static final String REF_ESTATE_LOAN_INTEREST = "ESTATE_LOAN_INTEREST";
    public static final String INTEREST_EXPENSE_TYPE = "Interest";

    private final EstateLoanRepository estateLoanRepository;
    private final EstateLoanTransactionRepository estateLoanTransactionRepository;
    private final MonetaryAssetTransactionRepository ledgerRepository;
    private final LedgerService ledgerService;
    private final ExpenseService expenseService;

    public List<EstateLoanResponse> getEstateLoans() {
        return estateLoanRepository.findAll().stream()
                .map(EstateLoanMapper::toResponse)
                .collect(Collectors.toList());
    }

    public EstateLoanResponse getEstateLoan(String loanId) {
        return EstateLoanMapper.toResponse(findLoan(loanId));
    }

    public List<EstateLoanMovementResponse> getMovements(String loanId) {
        findLoan(loanId);
        List<EstateLoanTransaction> txs = estateLoanTransactionRepository.findByLoanIdOrderBySeqAsc(loanId);
        List<EstateLoanMovementResponse> out = new ArrayList<>();
        for (int i = 0; i < txs.size(); i++) {
            out.add(EstateLoanMapper.toMovementResponse(txs.get(i), i == txs.size() - 1 && i > 0));
        }
        return out;
    }

    /** Taking a loan optionally books the money into a cash/bank account (ESTATE_LOAN_RECEIPT). */
    @Transactional
    public EstateLoanResponse createEstateLoan(EstateLoanRequest request) {
        EstateLoan loan = EstateLoanMapper.toEntity(request);
        String account = blankToNull(request.getReceivedIntoAccountCode());
        if (account != null) {
            ledgerService.requireCashOrBank(account);
        }
        EstateLoan saved = estateLoanRepository.save(loan);
        MonetaryAssetTransaction ledger = null;
        if (account != null) {
            ledger = ledgerService.post(account, LedgerDirection.IN, request.getPrincipalAmount(), request.getIssuedDate(),
                    LedgerCategory.ESTATE_LOAN_RECEIPT, REF_ESTATE_LOAN, saved.getLoanId(),
                    "Loan received: " + saved.getName());
        }
        saveMovement(saved.getLoanId(), "borrow", BigDecimal.ZERO, request.getPrincipalAmount(),
                request.getPrincipalAmount(), BigDecimal.ZERO, request.getIssuedDate(), ledger);
        return EstateLoanMapper.toResponse(saved);
    }

    @Transactional
    public EstateLoanResponse updateEstateLoan(String loanId, EstateLoanUpdateRequest request) {
        EstateLoan loan = findLoan(loanId);
        if (request.getName() != null && !request.getName().isBlank()) loan.setName(request.getName());
        if (request.getLender() != null) loan.setLender(blankToNull(request.getLender()));
        if (request.getInterestRate() != null) loan.setInterestRate(request.getInterestRate());
        if (request.getTermMonths() != null) loan.setTermMonths(request.getTermMonths());
        if (request.getInstallmentAmount() != null) loan.setInstallmentAmount(request.getInstallmentAmount());
        if (request.getNotes() != null) loan.setNotes(request.getNotes());
        return EstateLoanMapper.toResponse(estateLoanRepository.save(loan));
    }

    /** Removes a loan entered in error: only while it has just its opening borrowing; reverses the receipt. */
    @Transactional
    public void deleteEstateLoan(String loanId) {
        EstateLoan loan = findLoan(loanId);
        List<EstateLoanTransaction> txs = estateLoanTransactionRepository.findByLoanIdOrderBySeqAsc(loanId);
        if (txs.size() > 1) {
            throw new BadRequestException("Undo the repayments and drawdowns of this loan first");
        }
        for (EstateLoanTransaction tx : txs) {
            reverseLedger(tx, "Loan deleted: " + loan.getName());
            estateLoanTransactionRepository.delete(tx);
        }
        estateLoanRepository.delete(loan);
    }

    /** Additional drawdown on an existing loan; optionally received into a cash/bank account. */
    @Transactional
    public EstateLoanResponse recordDrawdown(String loanId, EstateLoanDrawdownRequest request) {
        EstateLoan loan = findLoan(loanId);
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();
        if (date.isBefore(loan.getIssuedDate())) {
            throw new BadRequestException("A drawdown cannot be dated before the loan was issued");
        }
        String account = blankToNull(request.getAccountCode());
        MonetaryAssetTransaction ledger = null;
        if (account != null) {
            ledgerService.requireCashOrBank(account);
            ledger = ledgerService.post(account, LedgerDirection.IN, request.getAmount(), date,
                    LedgerCategory.ESTATE_LOAN_RECEIPT, REF_ESTATE_LOAN, loanId, "Loan drawdown: " + loan.getName());
        }
        BigDecimal before = loan.getCurrentBalance();
        BigDecimal after = before.add(request.getAmount());
        saveMovement(loanId, "borrow", before, after, request.getAmount(), BigDecimal.ZERO, date, ledger);
        loan.setCurrentBalance(after);
        loan.setPrincipalAmount(loan.getPrincipalAmount().add(request.getAmount()));
        loan.setIsActive(true);
        return EstateLoanMapper.toResponse(estateLoanRepository.save(loan));
    }

    /**
     * Repayment: principal posts ESTATE_LOAN_REPAYMENT and reduces the balance; interest (if any) becomes an
     * "Interest" expense paid from the same account. The loan closes when its balance reaches zero.
     */
    @Transactional
    public EstateLoanResponse recordEstateLoanPayment(String loanId, EstateLoanPaymentRequest request) {
        EstateLoan loan = findLoan(loanId);
        BigDecimal principal = request.getPrincipalAmount();
        BigDecimal interest = request.getInterestAmount() != null ? request.getInterestAmount() : BigDecimal.ZERO;
        if (principal.signum() <= 0 && interest.signum() <= 0) {
            throw new BadRequestException("Enter a principal and/or interest amount to repay");
        }
        ledgerService.requireCashOrBank(request.getAccountCode());
        if (principal.compareTo(loan.getCurrentBalance()) > 0) {
            throw new InsufficientFundsException("Principal exceeds outstanding balance on loan " + loan.getName());
        }
        LocalDate date = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();

        MonetaryAssetTransaction ledger = null;
        if (principal.signum() > 0) {
            ledger = ledgerService.post(request.getAccountCode(), LedgerDirection.OUT, principal, date,
                    LedgerCategory.ESTATE_LOAN_REPAYMENT, REF_ESTATE_LOAN, loanId, "Loan repayment: " + loan.getName());
        }
        BigDecimal before = loan.getCurrentBalance();
        BigDecimal after = before.subtract(principal);
        EstateLoanTransaction tx = saveMovement(loanId, "repay", before, after, principal, interest, date, ledger);
        if (interest.signum() > 0) {
            expenseService.recordPaidSystemExpense(INTEREST_EXPENSE_TYPE, interest, date.atStartOfDay(),
                    request.getAccountCode(), LedgerCategory.EXPENSE_PAYMENT, REF_ESTATE_LOAN_INTEREST, tx.getId(),
                    "Loan interest: " + loan.getName());
        }
        loan.setCurrentBalance(after);
        if (after.signum() == 0) {
            loan.setIsActive(false);
        }
        return EstateLoanMapper.toResponse(estateLoanRepository.save(loan));
    }

    /** Undoes the latest repayment or drawdown (reverses the ledger rows and removes the interest expense). */
    @Transactional
    public EstateLoanResponse undoLastMovement(String loanId) {
        EstateLoan loan = findLoan(loanId);
        List<EstateLoanTransaction> txs = estateLoanTransactionRepository.findByLoanIdOrderBySeqAsc(loanId);
        if (txs.size() <= 1) {
            throw new BadRequestException("There is no repayment or drawdown to undo on this loan");
        }
        EstateLoanTransaction last = txs.get(txs.size() - 1);
        String reason = "Undo loan movement: " + loan.getName();
        if ("repay".equals(last.getTransactionType())) {
            expenseService.removeSystemExpenses(REF_ESTATE_LOAN_INTEREST, last.getId(), reason);
            reverseLedger(last, reason);
            loan.setCurrentBalance(loan.getCurrentBalance().add(last.getAmount()));
            loan.setIsActive(true);
        } else {
            reverseLedger(last, reason);
            loan.setCurrentBalance(loan.getCurrentBalance().subtract(last.getAmount()));
            loan.setPrincipalAmount(loan.getPrincipalAmount().subtract(last.getAmount()));
            if (loan.getCurrentBalance().signum() == 0) {
                loan.setIsActive(false);
            }
        }
        estateLoanTransactionRepository.delete(last);
        return EstateLoanMapper.toResponse(estateLoanRepository.save(loan));
    }

    // ── Reporting (reused by the reports wave) ──────────────────────────────

    /** Borrowings schedule: opening, drawdowns, principal repaid, interest paid and closing per loan (by movement date). */
    @Transactional(readOnly = true)
    public BorrowingScheduleResponse schedule(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("A valid from/to period is required");
        }
        List<EstateLoanTransaction> all = estateLoanTransactionRepository.findAllByOrderBySeqAsc();
        List<BorrowingRowResponse> rows = new ArrayList<>();
        for (EstateLoan loan : estateLoanRepository.findAll()) {
            if (loan.getIssuedDate().isAfter(to)) continue;
            BigDecimal opening = BigDecimal.ZERO;
            BigDecimal draws = BigDecimal.ZERO;
            BigDecimal repaid = BigDecimal.ZERO;
            BigDecimal interest = BigDecimal.ZERO;
            for (EstateLoanTransaction t : all) {
                if (!t.getLoanId().equals(loan.getLoanId())) continue;
                boolean borrow = "borrow".equals(t.getTransactionType());
                if (t.getTransactionDate().isBefore(from)) {
                    opening = borrow ? opening.add(t.getAmount()) : opening.subtract(t.getAmount());
                } else if (!t.getTransactionDate().isAfter(to)) {
                    if (borrow) {
                        draws = draws.add(t.getAmount());
                    } else {
                        repaid = repaid.add(t.getAmount());
                        interest = interest.add(t.getInterestAmount());
                    }
                }
            }
            BigDecimal closing = opening.add(draws).subtract(repaid);
            if (opening.signum() == 0 && draws.signum() == 0 && repaid.signum() == 0
                    && interest.signum() == 0 && closing.signum() == 0) {
                continue;
            }
            rows.add(BorrowingRowResponse.builder()
                    .loanId(loan.getLoanId()).name(loan.getName()).lender(loan.getLender())
                    .interestRate(loan.getInterestRate())
                    .openingBalance(opening).drawdowns(draws).principalRepaid(repaid)
                    .interestPaid(interest).closingBalance(closing)
                    .build());
        }
        rows.sort(Comparator.comparing(BorrowingRowResponse::getName));
        BorrowingRowResponse total = BorrowingRowResponse.builder()
                .openingBalance(sum(rows, BorrowingRowResponse::getOpeningBalance))
                .drawdowns(sum(rows, BorrowingRowResponse::getDrawdowns))
                .principalRepaid(sum(rows, BorrowingRowResponse::getPrincipalRepaid))
                .interestPaid(sum(rows, BorrowingRowResponse::getInterestPaid))
                .closingBalance(sum(rows, BorrowingRowResponse::getClosingBalance))
                .build();
        return BorrowingScheduleResponse.builder().from(from).to(to).loans(rows).total(total).build();
    }

    /** Outstanding balance of each loan on the date (loans not yet issued are omitted). */
    @Transactional(readOnly = true)
    public List<LoanBalanceAsOfResponse> balancesAsOf(LocalDate asOf) {
        List<EstateLoanTransaction> all = estateLoanTransactionRepository.findAllByOrderBySeqAsc();
        List<LoanBalanceAsOfResponse> out = new ArrayList<>();
        for (EstateLoan loan : estateLoanRepository.findAll()) {
            if (loan.getIssuedDate().isAfter(asOf)) continue;
            BigDecimal bal = BigDecimal.ZERO;
            for (EstateLoanTransaction t : all) {
                if (!t.getLoanId().equals(loan.getLoanId()) || t.getTransactionDate().isAfter(asOf)) continue;
                bal = "borrow".equals(t.getTransactionType()) ? bal.add(t.getAmount()) : bal.subtract(t.getAmount());
            }
            out.add(LoanBalanceAsOfResponse.builder().loanId(loan.getLoanId()).name(loan.getName())
                    .lender(loan.getLender()).balance(bal).build());
        }
        out.sort(Comparator.comparing(LoanBalanceAsOfResponse::getName));
        return out;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private EstateLoanTransaction saveMovement(String loanId, String type, BigDecimal before, BigDecimal after,
                                               BigDecimal amount, BigDecimal interest, LocalDate date,
                                               MonetaryAssetTransaction ledger) {
        EstateLoanTransaction tx = new EstateLoanTransaction();
        tx.setId(UUID.randomUUID().toString());
        tx.setLoanId(loanId);
        tx.setTransactionType(type);
        tx.setLastAmount(before);
        tx.setNewAmount(after);
        tx.setAmount(amount);
        tx.setInterestAmount(interest);
        tx.setTransactionDate(date);
        tx.setLedgerTransactionId(ledger != null ? ledger.getId() : null);
        return estateLoanTransactionRepository.saveAndFlush(tx);
    }

    private void reverseLedger(EstateLoanTransaction tx, String reason) {
        if (tx.getLedgerTransactionId() != null) {
            ledgerRepository.findById(tx.getLedgerTransactionId())
                    .ifPresent(entry -> ledgerService.reverseIfActive(entry, reason));
        }
    }

    private EstateLoan findLoan(String loanId) {
        return estateLoanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("EstateLoan", "id", loanId));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static BigDecimal sum(List<BorrowingRowResponse> rows, java.util.function.Function<BorrowingRowResponse, BigDecimal> f) {
        return rows.stream().map(f).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
