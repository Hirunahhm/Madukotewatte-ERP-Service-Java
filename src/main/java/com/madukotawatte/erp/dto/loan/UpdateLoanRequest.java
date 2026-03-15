package com.madukotawatte.erp.dto.loan;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateLoanRequest {
    private Boolean isActive;
    private BigDecimal currentBalance;
    private BigDecimal installment;
}
