package com.madukotawatte.erp.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpenseRequest {

    @NotBlank
    private String type;

    /** financial_accounts.code to pay from (cash, bank or credit card). Required when the expense is paid. */
    private String accountCode;

    /** Supplier / person to be paid. */
    @Size(max = 150)
    private String payee;

    @Size(max = 500)
    private String description;

    /** Date payment is expected (for unpaid expenses). */
    private LocalDate dueDate;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDateTime timestamp;

    /** Day the money left the account; defaults to the expense date (create) or the existing payment date (update). */
    private LocalDate paymentDate;

    /** Create: null means paid. Update: null keeps the current paid state. */
    private Boolean isPaid;
}
