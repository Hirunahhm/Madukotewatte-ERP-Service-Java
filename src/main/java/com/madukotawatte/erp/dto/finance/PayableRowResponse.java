package com.madukotawatte.erp.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayableRowResponse {
    private String expenseId;
    /** Standard expense category. */
    private String category;
    private String payee;
    private String description;
    private LocalDate expenseDate;
    private LocalDate dueDate;
    private BigDecimal amount;
    private long daysOutstanding;
    private String bucket;
    private boolean overdue;
}
