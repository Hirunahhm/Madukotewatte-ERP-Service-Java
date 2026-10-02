package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.account.AccountBalanceResponse;
import com.madukotawatte.erp.dto.account.AccountTrendPointResponse;
import com.madukotawatte.erp.dto.account.LedgerEntryResponse;
import com.madukotawatte.erp.dto.account.StatementResponse;
import com.madukotawatte.erp.dto.account.TransferResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.entity.AccountType;
import com.madukotawatte.erp.entity.FinancialAccount;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.InsufficientFundsException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.LedgerEntryMapper;
import com.madukotawatte.erp.repository.FinancialAccountRepository;
import com.madukotawatte.erp.repository.MonetaryAssetTransactionRepository;
import com.madukotawatte.erp.service.support.TrendSupport;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The single writer of the account ledger (monetary_asset_transactions). Every money movement in the
 * system goes through {@link #post}; undo is {@link #reverse} — ledger rows are never deleted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    public static final String REF_TRANSFER = "TRANSFER";

    private final FinancialAccountRepository accountRepository;
    private final MonetaryAssetTransactionRepository ledgerRepository;

    // ── Posting ─────────────────────────────────────────────────────────────

    /**
     * Posts one ledger row. Locks the account row, derives the running balance from the latest row only,
     * enforces cash/bank &gt;= 0 and card &gt;= -creditLimit and rejects inactive accounts.
     * Joins the caller's transaction so business record + ledger row commit (or fail) together.
     */
    @Transactional
    public MonetaryAssetTransaction post(String accountCode, LedgerDirection direction, BigDecimal amount,
                                         LocalDate date, LedgerCategory category,
                                         String referenceType, String referenceId, String description) {
        return postInternal(accountCode, direction, amount, date, category, referenceType, referenceId,
                description, null, false);
    }

    private MonetaryAssetTransaction postInternal(String accountCode, LedgerDirection direction, BigDecimal amount,
                                                  LocalDate date, LedgerCategory category,
                                                  String referenceType, String referenceId, String description,
                                                  String reversalOfId, boolean allowInactive) {
        if (accountCode == null || accountCode.isBlank()) {
            throw new BadRequestException("An account is required");
        }
        if (direction == null || category == null) {
            throw new BadRequestException("Ledger direction and category are required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
        FinancialAccount account = accountRepository.findByCodeForUpdate(accountCode)
                .orElseThrow(() -> new BadRequestException("Unknown account: " + accountCode));
        if (!Boolean.TRUE.equals(account.getIsActive()) && !allowInactive && category != LedgerCategory.OPENING_BALANCE) {
            throw new BadRequestException("Account " + accountCode + " is inactive");
        }

        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal last = ledgerRepository.findFirstByAccountCodeOrderBySeqDesc(accountCode)
                .map(MonetaryAssetTransaction::getNewAmount)
                .orElse(BigDecimal.ZERO);
        BigDecimal next = direction == LedgerDirection.IN ? last.add(scaled) : last.subtract(scaled);

        if (account.getType() == AccountType.CREDIT_CARD) {
            BigDecimal limit = account.getCreditLimit() != null ? account.getCreditLimit() : BigDecimal.ZERO;
            if (next.compareTo(limit.negate()) < 0) {
                throw new InsufficientFundsException("Credit limit exceeded on " + accountCode
                        + " (available credit " + limit.add(last).setScale(2, RoundingMode.HALF_UP) + ")");
            }
        } else if (next.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + accountCode
                    + " (balance " + last.setScale(2, RoundingMode.HALF_UP) + ")");
        }

        MonetaryAssetTransaction entry = new MonetaryAssetTransaction();
        entry.setId(UUID.randomUUID().toString());
        entry.setTransactionType(direction.getDbValue());
        entry.setAccountCode(accountCode);
        entry.setAmount(scaled);
        entry.setLastAmount(last);
        entry.setNewAmount(next);
        entry.setTransactionDate(date != null ? date : LocalDate.now());
        entry.setCategory(category);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setDescription(description);
        entry.setReversalOfId(reversalOfId);
        return ledgerRepository.saveAndFlush(entry);
    }

    /**
     * Undoes a ledger row by posting the opposite-direction row with the same category (so category totals net
     * to zero) and reversal_of_id set. The reversal keeps the original business date. Both legs of a transfer
     * are reversed together. Returns the reversal of the requested row.
     */
    @Transactional
    public MonetaryAssetTransaction reverse(String ledgerId, String reason) {
        MonetaryAssetTransaction original = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerEntry", "id", ledgerId));
        assertReversible(original);

        List<MonetaryAssetTransaction> legs = new ArrayList<>();
        legs.add(original);
        if (REF_TRANSFER.equals(original.getReferenceType()) && original.getReferenceId() != null) {
            for (MonetaryAssetTransaction other : ledgerRepository.findByReferenceTypeAndReferenceId(
                    REF_TRANSFER, original.getReferenceId())) {
                if (!other.getId().equals(original.getId()) && other.getReversalOfId() == null
                        && ledgerRepository.findByReversalOfId(other.getId()).isEmpty()) {
                    legs.add(other);
                }
            }
        }
        MonetaryAssetTransaction result = null;
        for (MonetaryAssetTransaction leg : legs) {
            MonetaryAssetTransaction reversal = postInternal(leg.getAccountCode(), leg.getDirection().opposite(),
                    leg.getAmount(), leg.getTransactionDate(), leg.getCategory(),
                    leg.getReferenceType(), leg.getReferenceId(), reversalDescription(leg, reason),
                    leg.getId(), true);
            if (leg == original) result = reversal;
        }
        return result;
    }

    private void assertReversible(MonetaryAssetTransaction entry) {
        if (entry.getReversalOfId() != null) {
            throw new BadRequestException("A reversal entry cannot itself be reversed");
        }
        if (ledgerRepository.findByReversalOfId(entry.getId()).isPresent()) {
            throw new BadRequestException("This entry has already been reversed");
        }
    }

    private String reversalDescription(MonetaryAssetTransaction original, String reason) {
        String base = "Reversal of " + original.getCategory() + " " + original.getTransactionDate();
        return reason != null && !reason.isBlank() ? base + ": " + reason.trim() : base;
    }

    /** True when {@code entry} has not been reversed and is not itself a reversal. */
    public boolean isActive(MonetaryAssetTransaction entry) {
        return entry.getReversalOfId() == null && ledgerRepository.findByReversalOfId(entry.getId()).isEmpty();
    }

    /** Reverses the entry only if it is still active; no-op for null / already reversed. */
    @Transactional
    public void reverseIfActive(MonetaryAssetTransaction entry, String reason) {
        if (entry != null && isActive(entry)) {
            reverse(entry.getId(), reason);
        }
    }

    /**
     * Moves money between two accounts. Target a credit card => CREDIT_CARD_PAYMENT, otherwise TRANSFER.
     * Both legs share reference_type 'TRANSFER' and one reference_id.
     */
    @Transactional
    public TransferResponse transfer(String fromCode, String toCode, BigDecimal amount, LocalDate date, String description) {
        if (fromCode == null || toCode == null || fromCode.equals(toCode)) {
            throw new BadRequestException("Choose two different accounts");
        }
        FinancialAccount from = getAccount(fromCode);
        FinancialAccount to = getAccount(toCode);
        if (from.getType() == AccountType.CREDIT_CARD) {
            throw new BadRequestException("Money cannot be transferred out of a credit card");
        }
        LedgerCategory category = to.getType() == AccountType.CREDIT_CARD
                ? LedgerCategory.CREDIT_CARD_PAYMENT : LedgerCategory.TRANSFER;
        String transferId = UUID.randomUUID().toString();
        LocalDate when = date != null ? date : LocalDate.now();
        String text = description != null && !description.isBlank() ? description.trim()
                : (category == LedgerCategory.CREDIT_CARD_PAYMENT ? "Credit card payment" : "Transfer")
                + " " + fromCode + " -> " + toCode;

        // lock in a fixed order so two opposite transfers cannot deadlock
        MonetaryAssetTransaction out;
        MonetaryAssetTransaction in;
        if (fromCode.compareTo(toCode) < 0) {
            out = postInternal(fromCode, LedgerDirection.OUT, amount, when, category, REF_TRANSFER, transferId, text, null, false);
            in = postInternal(toCode, LedgerDirection.IN, amount, when, category, REF_TRANSFER, transferId, text, null, false);
        } else {
            in = postInternal(toCode, LedgerDirection.IN, amount, when, category, REF_TRANSFER, transferId, text, null, false);
            out = postInternal(fromCode, LedgerDirection.OUT, amount, when, category, REF_TRANSFER, transferId, text, null, false);
        }
        return TransferResponse.builder()
                .transferId(transferId)
                .from(LedgerEntryMapper.toResponse(out, false, out.getNewAmount()))
                .to(LedgerEntryMapper.toResponse(in, false, in.getNewAmount()))
                .build();
    }

    // ── Accounts helpers ────────────────────────────────────────────────────

    public FinancialAccount getAccount(String code) {
        return accountRepository.findById(code)
                .orElseThrow(() -> new BadRequestException("Unknown account: " + code));
    }

    /** Validates that the account exists and is active; returns it. */
    public FinancialAccount requireActiveAccount(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("An account is required");
        }
        FinancialAccount account = getAccount(code);
        if (!Boolean.TRUE.equals(account.getIsActive())) {
            throw new BadRequestException("Account " + code + " is inactive");
        }
        return account;
    }

    /** Like {@link #requireActiveAccount} but also rejects credit cards (salary, labour, sale receipts, loans). */
    public FinancialAccount requireCashOrBank(String code) {
        FinancialAccount account = requireActiveAccount(code);
        if (account.getType() == AccountType.CREDIT_CARD) {
            throw new BadRequestException("A credit card cannot be used here; choose a cash or bank account");
        }
        return account;
    }

    // ── Balances ────────────────────────────────────────────────────────────

    public BigDecimal getBalance(String accountCode) {
        return ledgerRepository.findFirstByAccountCodeOrderBySeqDesc(accountCode)
                .map(MonetaryAssetTransaction::getNewAmount)
                .orElse(BigDecimal.ZERO);
    }

    public Map<String, BigDecimal> getCurrentBalanceMap() {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Object[] row : ledgerRepository.findCurrentBalances()) {
            map.put((String) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    /** asOf == null: current balances; otherwise opening rows + signed sum of amounts with transaction_date &lt;= asOf. */
    public List<AccountBalanceResponse> getBalances(LocalDate asOf) {
        Map<String, BigDecimal> balances = new HashMap<>();
        if (asOf == null) {
            balances.putAll(getCurrentBalanceMap());
        } else {
            for (Object[] row : ledgerRepository.findBalancesAsOf(asOf)) {
                balances.put((String) row[0], (BigDecimal) row[1]);
            }
        }
        return accountRepository.findAll(Sort.by("type", "code")).stream()
                .map(a -> {
                    BigDecimal balance = balances.getOrDefault(a.getCode(), BigDecimal.ZERO);
                    return AccountBalanceResponse.builder()
                            .accountCode(a.getCode())
                            .name(a.getName())
                            .type(a.getType())
                            .isActive(a.getIsActive())
                            .balance(balance)
                            .creditLimit(a.getCreditLimit())
                            .availableCredit(LedgerEntryMapper.availableCredit(a, balance))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** Cash+bank and credit-card balance at the end of each bucket of the week / month / year window. */
    public List<AccountTrendPointResponse> getTrend(String scale) {
        TrendSupport.Window window = TrendSupport.resolveWindow(scale);
        List<String> labels = TrendSupport.labels(scale);
        List<LocalDate> boundaries = TrendSupport.bucketEndDates(scale, window);
        List<Object[]> rows = ledgerRepository.findDailyTotalsByType(boundaries.get(boundaries.size() - 1));

        BigDecimal cashAndBank = BigDecimal.ZERO;
        BigDecimal cards = BigDecimal.ZERO;
        int idx = 0;
        List<AccountTrendPointResponse> points = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            while (idx < rows.size()) {
                Object[] row = rows.get(idx);
                if (TrendSupport.toLocalDate(row[0]).isAfter(boundaries.get(i))) break;
                BigDecimal sum = (BigDecimal) row[2];
                if (AccountType.CREDIT_CARD.name().equals(row[1])) cards = cards.add(sum);
                else cashAndBank = cashAndBank.add(sum);
                idx++;
            }
            points.add(AccountTrendPointResponse.builder()
                    .name(labels.get(i)).cashAndBank(cashAndBank).creditCards(cards).net(cashAndBank.add(cards))
                    .build());
        }
        return points;
    }

    // ── Reads ───────────────────────────────────────────────────────────────

    public PageResponse<LedgerEntryResponse> getLedger(String accountCode, LedgerCategory category,
                                                       LocalDate from, LocalDate to, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Order.desc("transactionDate"), Sort.Order.desc("seq")));
        var result = ledgerRepository.findAll(ledgerSpec(accountCode, category, from, to), pageable);
        Set<String> reversed = reversedIds(result.getContent());
        return PageResponse.from(result.map(e ->
                LedgerEntryMapper.toResponse(e, reversed.contains(e.getId()), e.getNewAmount())));
    }

    public StatementResponse getStatement(String accountCode, LocalDate from, LocalDate to, LedgerCategory category) {
        FinancialAccount account = accountRepository.findById(accountCode)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "code", accountCode));
        if (from == null || to == null || from.isAfter(to)) {
            throw new BadRequestException("A valid from/to date range is required");
        }
        BigDecimal opening = ledgerRepository.sumBefore(accountCode, from);
        List<MonetaryAssetTransaction> entries = ledgerRepository.findAll(
                ledgerSpec(accountCode, null, from, to),
                Sort.by(Sort.Order.asc("transactionDate"), Sort.Order.asc("seq")));
        Set<String> reversed = reversedIds(entries);

        BigDecimal running = opening;
        BigDecimal totalIn = BigDecimal.ZERO;
        BigDecimal totalOut = BigDecimal.ZERO;
        List<LedgerEntryResponse> rows = new ArrayList<>();
        for (MonetaryAssetTransaction e : entries) {
            running = running.add(e.getSignedAmount());
            if (e.getDirection() == LedgerDirection.IN) totalIn = totalIn.add(e.getAmount());
            else totalOut = totalOut.add(e.getAmount());
            if (category == null || e.getCategory() == category) {
                rows.add(LedgerEntryMapper.toResponse(e, reversed.contains(e.getId()), running));
            }
        }
        return StatementResponse.builder()
                .accountCode(account.getCode())
                .name(account.getName())
                .type(account.getType())
                .from(from)
                .to(to)
                .openingBalance(opening)
                .totalIn(totalIn)
                .totalOut(totalOut)
                .closingBalance(running)
                .rows(rows)
                .build();
    }

    public LedgerEntryResponse getEntry(String id) {
        MonetaryAssetTransaction e = ledgerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LedgerEntry", "id", id));
        return LedgerEntryMapper.toResponse(e, ledgerRepository.findByReversalOfId(id).isPresent(), e.getNewAmount());
    }

    public Set<String> reversedIds(Collection<MonetaryAssetTransaction> entries) {
        if (entries.isEmpty()) return Set.of();
        return new HashSet<>(ledgerRepository.findReversedIds(
                entries.stream().map(MonetaryAssetTransaction::getId).collect(Collectors.toList())));
    }

    private Specification<MonetaryAssetTransaction> ledgerSpec(String accountCode, LedgerCategory category,
                                                               LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (accountCode != null && !accountCode.isBlank()) {
                predicates.add(cb.equal(root.get("accountCode"), accountCode));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
