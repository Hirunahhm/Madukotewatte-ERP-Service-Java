package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/** Movement on a named estate loan (credit cards are ledger accounts since V17). */
@Entity
@Table(name = "estate_loan_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstateLoanTransaction extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "loan_id", nullable = false, length = 36)
    private String loanId;

    // 'borrow' | 'repay'
    @Column(name = "transaction_type", length = 10)
    private String transactionType;

    @Column(name = "last_amount", precision = 15, scale = 2)
    private BigDecimal lastAmount;

    @Column(name = "new_amount", precision = 15, scale = 2)
    private BigDecimal newAmount;
}
