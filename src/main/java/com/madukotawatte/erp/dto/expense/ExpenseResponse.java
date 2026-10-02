package com.madukotawatte.erp.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseResponse {
    private String expenseId;
    private String type;
    private String accountCode;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    /** Ledger row that paid this expense (null while unpaid). */
    private String transactionId;
    /** Business date of the payment (null while unpaid). */
    private LocalDate paymentDate;
    /** Set on system-created expenses, e.g. LABOUR. */
    private String referenceType;
    private String referenceId;
    private Boolean isPaid;
    private String status;
    private LocalDateTime createdAt;
}
