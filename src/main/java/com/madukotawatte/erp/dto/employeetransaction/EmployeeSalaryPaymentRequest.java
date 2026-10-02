package com.madukotawatte.erp.dto.employeetransaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EmployeeSalaryPaymentRequest {
    @NotBlank
    private String employeeId;

    /** financial_accounts.code the salary is paid from (cash or bank). */
    @NotBlank
    private String accountCode;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private LocalDateTime timestamp;
}
