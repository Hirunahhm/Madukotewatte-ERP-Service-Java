package com.madukotawatte.erp.dto.labour;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LabourRequest {
    @NotBlank
    private String employeeId;

    private String transactionRecordId;

    private Boolean isPaid = false;

    @NotNull
    @Positive
    private BigDecimal workedHours;

    // optional: defaults to the employee's hourly rate
    @Positive
    private BigDecimal hourlyRate;

    // optional: computed server-side as workedHours x hourlyRate when omitted
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String workType;

    private String description;

    private LocalDateTime timestamp;

    // financial_accounts.code (cash or bank) — required only when isPaid=true (validated in service layer)
    private String accountCode;
}
