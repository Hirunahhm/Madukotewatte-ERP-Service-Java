package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.account.AccountResponse;
import com.madukotawatte.erp.dto.account.LedgerEntryResponse;
import com.madukotawatte.erp.entity.AccountType;
import com.madukotawatte.erp.entity.FinancialAccount;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;

import java.math.BigDecimal;
import java.util.Set;

public class LedgerEntryMapper {

    /** Reference types whose ledger rows are owned by the account screens (not by a business record). */
    private static final Set<String> USER_REVERSIBLE_REFERENCES = Set.of("MANUAL", "TRANSFER");

    private LedgerEntryMapper() {}

    public static LedgerEntryResponse toResponse(MonetaryAssetTransaction entry, boolean reversed, BigDecimal balanceAfter) {
        boolean reversible = entry.getReversalOfId() == null && !reversed
                && (entry.getReferenceType() == null || USER_REVERSIBLE_REFERENCES.contains(entry.getReferenceType()));
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .accountCode(entry.getAccountCode())
                .transactionType(entry.getTransactionType())
                .category(entry.getCategory())
                .amount(entry.getAmount())
                .signedAmount(entry.getSignedAmount())
                .transactionDate(entry.getTransactionDate())
                .referenceType(entry.getReferenceType())
                .referenceId(entry.getReferenceId())
                .description(entry.getDescription())
                .reversalOfId(entry.getReversalOfId())
                .reversed(reversed)
                .reversible(reversible)
                .balanceAfter(balanceAfter)
                .createdAt(entry.getCreatedAt())
                .build();
    }

    public static AccountResponse toResponse(FinancialAccount account, BigDecimal balance) {
        return AccountResponse.builder()
                .code(account.getCode())
                .name(account.getName())
                .type(account.getType())
                .institution(account.getInstitution())
                .accountNumber(account.getAccountNumber())
                .creditLimit(account.getCreditLimit())
                .openingBalance(account.getOpeningBalance())
                .openingDate(account.getOpeningDate())
                .isActive(account.getIsActive())
                .balance(balance)
                .availableCredit(availableCredit(account, balance))
                .createdAt(account.getCreatedAt())
                .build();
    }

    public static BigDecimal availableCredit(FinancialAccount account, BigDecimal balance) {
        return account.getType() == AccountType.CREDIT_CARD && account.getCreditLimit() != null
                ? account.getCreditLimit().add(balance)
                : null;
    }
}
