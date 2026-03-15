package com.madukotawatte.erp.dto.latex;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class LatexRecordRequest {
    @NotBlank
    private String loadId;

    @NotBlank
    private String employeeId;

    @NotNull
    private LocalDateTime timestamp;

    @NotNull
    @Positive
    private BigDecimal latexAmount;

    @NotNull
    @Positive
    private BigDecimal ammoniaAmount;

    private String metrolacReadingId;
}
