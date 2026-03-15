package com.madukotawatte.erp.dto.metrolac;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MetrolacReadingRequest {
    @NotBlank
    private String loadId;

    @NotNull
    private BigDecimal temperature;

    @NotNull
    private LocalDateTime timestamp;
}
