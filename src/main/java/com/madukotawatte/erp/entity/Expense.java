package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Expense extends BaseEntity {

    @Id
    @Column(name = "expense_id", length = 36)
    private String expenseId;

    @Column(length = 100)
    private String type;

    // 'Credit Card-Peoples' | 'Cash' | 'Bank Transfer-BOC' | etc.
    @Column(name = "payment_type", length = 40, nullable = false)
    private String paymentType;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estate_loan_transaction_id")
    private EstateLoanTransaction estateLoanTransaction;

    private LocalDateTime timestamp;
}
