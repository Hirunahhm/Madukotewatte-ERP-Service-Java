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
public class CreditCardLimitResponse {
    private String loanType;
    private BigDecimal creditLimit;
    private BigDecimal balance;
    private BigDecimal availableCredit;
}
