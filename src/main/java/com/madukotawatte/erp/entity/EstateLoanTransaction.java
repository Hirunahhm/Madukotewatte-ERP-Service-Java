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

    // Set for legacy fixed-bucket transactions: 'credit-card - Peoples' | 'credit-card - Sampath'
    @Column(name = "loan_type", length = 30)
    private String loanType;

    // Set for named-loan transactions (see EstateLoan) — exactly one of loanType/loanId is set
    @Column(name = "loan_id", length = 36)
    private String loanId;

    // 'borrow' | 'repay'
    @Column(name = "transaction_type", length = 10)
    private String transactionType;

    @Column(name = "last_amount", precision = 15, scale = 2)
    private BigDecimal lastAmount;

    @Column(name = "new_amount", precision = 15, scale = 2)
    private BigDecimal newAmount;
}
