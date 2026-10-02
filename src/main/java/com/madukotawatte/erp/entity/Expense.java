package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // financial_accounts.code the expense was paid from — null until the expense is paid
    @Column(name = "account_code", length = 40)
    private String accountCode;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;

    // set on system-created expenses (LABOUR, PAYSLIP, ...) so they can be traced and not double counted
    @Column(name = "reference_type", length = 40)
    private String referenceType;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    // who is owed the money (supplier / contractor); free text
    @Column(length = 150)
    private String payee;

    @Column(length = 500)
    private String description;

    // date the supplier expects payment (credit purchases)
    @Column(name = "due_date")
    private LocalDate dueDate;

    private LocalDateTime timestamp;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = true;

    @Column(nullable = false, length = 20)
    private String status = "paid";
}
