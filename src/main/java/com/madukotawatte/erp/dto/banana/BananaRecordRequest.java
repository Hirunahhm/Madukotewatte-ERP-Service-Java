package com.madukotawatte.erp.dto.banana;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BananaRecordRequest {
    @NotBlank
    private String loadId;

    @NotBlank
    private String employeeId;

    @NotBlank
    private String variety;

    private String varietyNote;

    private Integer bunchCount;

    private BigDecimal massKg;

    @NotNull
    private LocalDateTime timestamp;
}
