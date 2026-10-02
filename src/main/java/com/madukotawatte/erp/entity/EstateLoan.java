package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "estate_loans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstateLoan extends BaseEntity {

    @Id
    @Column(name = "loan_id", length = 36)
    private String loanId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "principal_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "current_balance", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentBalance;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "interest_rate", precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(length = 150)
    private String lender;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "installment_amount", precision = 15, scale = 2)
    private BigDecimal installmentAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
