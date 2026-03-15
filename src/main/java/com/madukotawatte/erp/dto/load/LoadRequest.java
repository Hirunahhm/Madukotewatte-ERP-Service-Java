package com.madukotawatte.erp.dto.load;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LoadRequest {
    @NotBlank
    private String loadType;

    @NotNull
    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
