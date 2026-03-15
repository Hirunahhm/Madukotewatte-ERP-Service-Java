package com.madukotawatte.erp.dto.metrolac;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetrolacReadingResponse {
    private String metrolacId;
    private String loadId;
    private BigDecimal temperature;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
