package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employee_loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeLoan extends BaseEntity {

    @Id
    @Column(name = "loan_id", length = 36)
    private String loanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "principal_amount", precision = 15, scale = 2)
    private BigDecimal principalAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal interest;

    @Column(precision = 10, scale = 2)
    private BigDecimal installment;

    @Column(name = "current_balance", precision = 15, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    // ledger row of the EMPLOYEE_LOAN_ISSUE posting (null for pre-ledger loans = opening balances)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
