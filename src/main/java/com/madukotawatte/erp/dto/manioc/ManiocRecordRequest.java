package com.madukotawatte.erp.dto.manioc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ManiocRecordRequest {
    @NotBlank
    private String loadId;

    @NotBlank
    private String employeeId;

    @NotBlank
    private String variety;

    private String varietyNote;

    @NotNull
    private BigDecimal massKg;

    @NotNull
    private LocalDateTime timestamp;
}
