package com.madukotawatte.erp.dto.ammonia;

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
public class AmmoniaRecordResponse {
    private String recordId;
    private String type;
    private BigDecimal litres;
    private LocalDateTime timestamp;
    private LocalDateTime createdAt;
}
