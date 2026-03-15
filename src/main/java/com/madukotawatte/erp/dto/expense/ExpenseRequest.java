package com.madukotawatte.erp.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpenseRequest {
    @NotBlank
    private String type;

    @NotBlank
    private String paymentType;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDateTime timestamp;

    private String monetaryTransactionId;
    private String estateLoanTransactionId;
}
