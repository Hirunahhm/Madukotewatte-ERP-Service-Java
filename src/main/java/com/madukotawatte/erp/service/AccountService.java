package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.account.AccountCreateRequest;
import com.madukotawatte.erp.dto.account.AccountResponse;
import com.madukotawatte.erp.dto.account.AccountUpdateRequest;
import com.madukotawatte.erp.dto.account.LedgerEntryResponse;
import com.madukotawatte.erp.dto.account.ManualEntryRequest;
import com.madukotawatte.erp.entity.AccountType;
import com.madukotawatte.erp.entity.FinancialAccount;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.DuplicateResourceException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.LedgerEntryMapper;
import com.madukotawatte.erp.repository.FinancialAccountRepository;
import com.madukotawatte.erp.repository.MonetaryAssetTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Account master data (create / edit / list) and manual ledger entries. */
@Service
@RequiredArgsConstructor
public class AccountService {

    public static final String REF_ACCOUNT = "ACCOUNT";
    public static final String REF_MANUAL = "MANUAL";

    private static final Map<LedgerCategory, LedgerDirection> MANUAL_CATEGORY_DIRECTION = new EnumMap<>(LedgerCategory.class);
    static {
        MANUAL_CATEGORY_DIRECTION.put(LedgerCategory.OWNER_CAPITAL, LedgerDirection.IN);
        MANUAL_CATEGORY_DIRECTION.put(LedgerCategory.OWNER_DRAWING, LedgerDirection.OUT);
        MANUAL_CATEGORY_DIRECTION.put(LedgerCategory.OTHER_INCOME, LedgerDirection.IN);
        MANUAL_CATEGORY_DIRECTION.put(LedgerCategory.INTEREST_INCOME, LedgerDirection.IN);
        MANUAL_CATEGORY_DIRECTION.put(LedgerCategory.BANK_CHARGES, LedgerDirection.OUT);
    }
    private static final Set<LedgerCategory> MANUAL_CATEGORIES = Set.of(
            LedgerCategory.OWNER_CAPITAL, LedgerCategory.OWNER_DRAWING, LedgerCategory.OTHER_INCOME,
            LedgerCategory.INTEREST_INCOME, LedgerCategory.BANK_CHARGES, LedgerCategory.ADJUSTMENT);

    private final FinancialAccountRepository accountRepository;
    private final MonetaryAssetTransactionRepository ledgerRepository;
    private final LedgerService ledgerService;

    public List<AccountResponse> getAccounts() {
        Map<String, BigDecimal> balances = ledgerService.getCurrentBalanceMap();
        return accountRepository.findAll(Sort.by("type", "code")).stream()
                .map(a -> LedgerEntryMapper.toResponse(a, balances.getOrDefault(a.getCode(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(String code) {
        FinancialAccount account = accountRepository.findById(code)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "code", code));
        return LedgerEntryMapper.toResponse(account, ledgerService.getBalance(code));
    }

    @Transactional
    public AccountResponse createAccount(AccountCreateRequest request) {
        if (accountRepository.existsById(request.getCode())) {
            throw new DuplicateResourceException("An account with code '" + request.getCode() + "' already exists");
        }
        BigDecimal opening = request.getOpeningBalance() != null
                ? request.getOpeningBalance().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        LocalDate openingDate = request.getOpeningDate() != null ? request.getOpeningDate() : LocalDate.now();
        BigDecimal creditLimit = normaliseLimit(request.getType(), request.getCreditLimit());
        validateOpening(request.getType(), opening, creditLimit);

        FinancialAccount account = FinancialAccount.builder()
                .code(request.getCode())
                .name(request.getName().trim())
                .type(request.getType())
                .institution(blankToNull(request.getInstitution()))
                .accountNumber(blankToNull(request.getAccountNumber()))
                .creditLimit(creditLimit)
                .openingBalance(opening)
                .openingDate(openingDate)
                .isActive(true)
                .build();
        accountRepository.saveAndFlush(account);
        postOpening(account.getCode(), opening, openingDate);
        return getAccount(account.getCode());
    }

    @Transactional
    public AccountResponse updateAccount(String code, AccountUpdateRequest request) {
        FinancialAccount account = accountRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "code", code));
        BigDecimal balance = ledgerService.getBalance(code);

        if (request.getName() != null) {
            if (request.getName().isBlank()) throw new BadRequestException("Name cannot be blank");
            account.setName(request.getName().trim());
        }
        if (request.getInstitution() != null) account.setInstitution(blankToNull(request.getInstitution()));
        if (request.getAccountNumber() != null) account.setAccountNumber(blankToNull(request.getAccountNumber()));

        if (request.getCreditLimit() != null) {
            if (account.getType() != AccountType.CREDIT_CARD) {
                throw new BadRequestException("Only credit cards have a credit limit");
            }
            BigDecimal newLimit = normaliseLimit(account.getType(), request.getCreditLimit());
            if (newLimit.add(balance).compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Credit limit cannot be lower than the amount currently owed ("
                        + balance.negate().setScale(2, RoundingMode.HALF_UP) + ")");
            }
            account.setCreditLimit(newLimit);
        }

        if (request.getIsActive() != null && !request.getIsActive().equals(account.getIsActive())) {
            if (!request.getIsActive() && balance.compareTo(BigDecimal.ZERO) != 0) {
                throw new BadRequestException("Account " + code + " still has a balance of "
                        + balance.setScale(2, RoundingMode.HALF_UP) + "; move it out before deactivating");
            }
            account.setIsActive(request.getIsActive());
        }

        BigDecimal newOpening = request.getOpeningBalance() != null
                ? request.getOpeningBalance().setScale(2, RoundingMode.HALF_UP) : account.getOpeningBalance();
        LocalDate newDate = request.getOpeningDate() != null ? request.getOpeningDate() : account.getOpeningDate();
        boolean openingChanged = newOpening.compareTo(account.getOpeningBalance()) != 0
                || !newDate.equals(account.getOpeningDate());
        if (openingChanged) {
            validateOpening(account.getType(), newOpening, account.getCreditLimit());
            List<MonetaryAssetTransaction> previous = ledgerRepository
                    .findByAccountCodeAndCategoryOrderBySeqAsc(code, LedgerCategory.OPENING_BALANCE).stream()
                    .filter(e -> e.getReversalOfId() == null)
                    .collect(Collectors.toList());
            Set<String> reversed = ledgerService.reversedIds(previous);
            // post the new row first: for the usual "raise the opening balance" case it can never fail the funds check
            postOpening(code, newOpening, newDate);
            for (MonetaryAssetTransaction old : previous) {
                if (!reversed.contains(old.getId())) {
                    ledgerService.reverse(old.getId(), "Opening balance changed");
                }
            }
            account.setOpeningBalance(newOpening);
            account.setOpeningDate(newDate);
        }
        accountRepository.saveAndFlush(account);
        return getAccount(code);
    }

    /** Capital in / drawings / other income / interest income / bank charges / adjustment. */
    @Transactional
    public LedgerEntryResponse createManualEntry(String code, ManualEntryRequest request) {
        if (!MANUAL_CATEGORIES.contains(request.getCategory())) {
            throw new BadRequestException("Category " + request.getCategory() + " cannot be posted manually; allowed: "
                    + MANUAL_CATEGORIES.stream().map(Enum::name).sorted().collect(Collectors.joining(", ")));
        }
        LedgerDirection required = MANUAL_CATEGORY_DIRECTION.get(request.getCategory());
        if (required != null && required != request.getDirection()) {
            throw new BadRequestException(request.getCategory() + " must be money " + (required == LedgerDirection.IN ? "in" : "out"));
        }
        MonetaryAssetTransaction entry = ledgerService.post(code, request.getDirection(), request.getAmount(),
                request.getDate(), request.getCategory(), REF_MANUAL, null, request.getDescription());
        return LedgerEntryMapper.toResponse(entry, false, entry.getNewAmount());
    }

    private void postOpening(String code, BigDecimal signedBalance, LocalDate date) {
        if (signedBalance.compareTo(BigDecimal.ZERO) == 0) return;
        ledgerService.post(code,
                signedBalance.signum() > 0 ? LedgerDirection.IN : LedgerDirection.OUT,
                signedBalance.abs(), date, LedgerCategory.OPENING_BALANCE, REF_ACCOUNT, code, "Opening balance");
    }

    private void validateOpening(AccountType type, BigDecimal opening, BigDecimal creditLimit) {
        if (type == AccountType.CREDIT_CARD) {
            if (opening.signum() > 0) {
                throw new BadRequestException("A credit card's opening balance is the amount owed, entered as zero or a negative number");
            }
            if (creditLimit != null && opening.negate().compareTo(creditLimit) > 0) {
                throw new BadRequestException("Opening balance owed exceeds the credit limit");
            }
        } else if (opening.signum() < 0) {
            throw new BadRequestException("Opening balance of a cash or bank account cannot be negative");
        }
    }

    private BigDecimal normaliseLimit(AccountType type, BigDecimal limit) {
        if (type == AccountType.CREDIT_CARD) {
            if (limit == null) throw new BadRequestException("creditLimit is required for a credit card");
            if (limit.signum() < 0) throw new BadRequestException("creditLimit cannot be negative");
            return limit.setScale(2, RoundingMode.HALF_UP);
        }
        if (limit != null) throw new BadRequestException("Only credit cards have a credit limit");
        return null;
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
