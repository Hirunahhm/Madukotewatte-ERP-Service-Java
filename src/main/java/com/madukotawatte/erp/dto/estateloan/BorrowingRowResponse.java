package com.madukotawatte.erp.dto.estateloan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Borrowings schedule line: opening + drawdowns - principal repaid = closing; interest is informational. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingRowResponse {
    private String loanId;
    private String name;
    private String lender;
    private BigDecimal interestRate;
    private BigDecimal openingBalance;
    private BigDecimal drawdowns;
    private BigDecimal principalRepaid;
    private BigDecimal interestPaid;
    private BigDecimal closingBalance;
}
