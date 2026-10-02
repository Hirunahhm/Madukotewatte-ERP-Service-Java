package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "financial_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FinancialAccount extends BaseEntity {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AccountType type;

    @Column(length = 100)
    private String institution;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    // credit cards only
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    // signed: negative for a card that already owed money on the opening date
    @Column(name = "opening_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal openingBalance;

    @Column(name = "opening_date", nullable = false)
    private LocalDate openingDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
