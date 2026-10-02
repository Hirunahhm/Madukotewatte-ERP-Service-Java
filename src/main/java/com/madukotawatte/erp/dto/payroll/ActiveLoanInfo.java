package com.madukotawatte.erp.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveLoanInfo {
    private String loanId;
    private BigDecimal balance;
    private BigDecimal installment;
}
