package com.madukotawatte.erp.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private String expenseId;
    private String type;
    private String paymentType;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String monetaryTransactionId;
    private String estateLoanTransactionId;
    private LocalDateTime createdAt;
}
