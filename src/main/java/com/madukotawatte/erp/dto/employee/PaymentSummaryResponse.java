package com.madukotawatte.erp.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    private BigDecimal totalSalaryCost;
    private BigDecimal toBePaid;
    private BigDecimal paidAmount;
    private BigDecimal activeEmployeeLoansTotal;
}
