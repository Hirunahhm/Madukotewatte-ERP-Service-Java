package com.madukotawatte.erp.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransferRequest {
    @NotBlank
    private String fromAccountCode;
    @NotBlank
    private String toAccountCode;
    @NotNull
    @Positive
    private BigDecimal amount;
    /** Defaults to today. */
    private LocalDate date;
    private String description;
}
