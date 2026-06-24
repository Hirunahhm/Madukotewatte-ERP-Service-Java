package com.madukotawatte.erp.dto.labour;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LabourRequest {
    @NotBlank
    private String employeeId;

    private String transactionRecordId;

    private Boolean isPaid = false;

    @NotNull
    @Positive
    private BigDecimal workedHours;

    @NotNull
    @Positive
    private BigDecimal hourlyRate;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotBlank
    private String workType;

    private String description;
}
