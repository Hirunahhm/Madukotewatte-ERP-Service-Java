package com.madukotawatte.erp.dto.employeetransaction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeTransactionStatsResponse {
    private BigDecimal totalAmount;
    private BigDecimal manualLabor;
    private BigDecimal advances;
    private BigDecimal loanPayments;
    private BigDecimal latexTap;
    private long transactionCount;
}
