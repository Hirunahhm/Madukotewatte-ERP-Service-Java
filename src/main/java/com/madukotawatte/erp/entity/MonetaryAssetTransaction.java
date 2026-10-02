package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the account ledger (table monetary_asset_transactions). Never deleted; undo = reversal row. */
@Entity
@Table(name = "monetary_asset_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonetaryAssetTransaction extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    // 'money in' | 'money out'
    @Column(name = "transaction_type", nullable = false, length = 10)
    private String transactionType;

    @Column(name = "account_code", nullable = false, length = 40)
    private String accountCode;

    // always positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "last_amount", precision = 15, scale = 2)
    private BigDecimal lastAmount;

    // running balance of the account after this row (negative = owed, for cards)
    @Column(name = "new_amount", precision = 15, scale = 2)
    private BigDecimal newAmount;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LedgerCategory category;

    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "reversal_of_id", length = 36)
    private String reversalOfId;

    // database-assigned insertion order, used to find the latest running balance
    @Column(name = "seq", insertable = false, updatable = false)
    private Long seq;

    public LedgerDirection getDirection() {
        return LedgerDirection.fromDbValue(transactionType);
    }

    public BigDecimal getSignedAmount() {
        return getDirection() == LedgerDirection.IN ? amount : amount.negate();
    }
}
