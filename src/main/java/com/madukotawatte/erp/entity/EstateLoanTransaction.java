package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "estate_loan_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstateLoanTransaction extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    // 'credit-card - Peoples' | 'credit-card - Sampath' | 'Loan-mom' | 'Loan-other'
    @Column(name = "loan_type", length = 30)
    private String loanType;

    @Column(name = "last_amount", precision = 15, scale = 2)
    private BigDecimal lastAmount;

    @Column(name = "new_amount", precision = 15, scale = 2)
    private BigDecimal newAmount;
}
