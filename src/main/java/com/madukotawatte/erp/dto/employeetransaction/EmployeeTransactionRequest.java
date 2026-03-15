package com.madukotawatte.erp.dto.employeetransaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EmployeeTransactionRequest {
    @NotBlank
    private String employeeId;

    @NotBlank
    private String type;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDateTime timestamp;
}
