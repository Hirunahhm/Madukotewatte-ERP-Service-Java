package com.madukotawatte.erp.dto.estateloan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One movement on a loan: type is "borrow" (initial or later drawdown) or "repay". */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstateLoanMovementResponse {
    private String id;
    private String loanId;
    private String type;
    private LocalDate transactionDate;
    /** Principal borrowed or repaid. */
    private BigDecimal amount;
    private BigDecimal interestAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String ledgerTransactionId;
    /** Latest movement of the loan, which can be undone. */
    private Boolean undoable;
}
