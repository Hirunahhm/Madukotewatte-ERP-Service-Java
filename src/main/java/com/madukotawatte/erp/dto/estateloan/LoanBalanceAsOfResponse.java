package com.madukotawatte.erp.dto.estateloan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanBalanceAsOfResponse {
    private String loanId;
    private String name;
    private String lender;
    private BigDecimal balance;
}
