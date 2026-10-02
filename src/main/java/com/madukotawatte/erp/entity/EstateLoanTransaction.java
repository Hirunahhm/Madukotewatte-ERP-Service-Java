package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    /** Business date of the movement; all as-of / schedule logic uses this. */
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    /** Principal moved (drawdown or principal repaid), always positive. */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Interest paid with a repayment (repay rows only). */
    @Column(name = "interest_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal interestAmount = BigDecimal.ZERO;

    /** Ledger row of the principal movement (null when no account was involved). */
    @Column(name = "ledger_transaction_id", length = 36)
    private String ledgerTransactionId;

    @Column(name = "seq", insertable = false, updatable = false)
    private Long seq;
}
