package com.madukotawatte.erp.dto.ammonia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AmmoniaRecordRequest {
    @NotBlank
    private String type;

    @NotNull
    @Positive
    private BigDecimal litres;

    @NotNull
    private LocalDateTime timestamp;
}
