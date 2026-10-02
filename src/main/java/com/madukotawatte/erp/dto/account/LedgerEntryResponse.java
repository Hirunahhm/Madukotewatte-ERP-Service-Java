package com.madukotawatte.erp.dto.account;

import com.madukotawatte.erp.entity.LedgerCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntryResponse {
    private String id;
    private String accountCode;
    /** 'money in' or 'money out'. */
    private String transactionType;
    private LedgerCategory category;
    private BigDecimal amount;
    /** +amount for money in, -amount for money out. */
    private BigDecimal signedAmount;
    private LocalDate transactionDate;
    private String referenceType;
    private String referenceId;
    private String description;
    private String reversalOfId;
    /** true when a reversal row exists for this entry. */
    private Boolean reversed;
    /** true when the entry may be reversed through POST /accounts/ledger/{id}/reverse. */
    private Boolean reversible;
    /** Running balance after this row. Ledger list: insertion order. Statement: date order. */
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}
