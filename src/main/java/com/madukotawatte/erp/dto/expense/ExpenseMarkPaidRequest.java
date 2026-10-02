package com.madukotawatte.erp.dto.expense;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ExpenseMarkPaidRequest {

    @NotBlank
    private String accountCode;

    /** Defaults to today. */
    private LocalDate paymentDate;
}
