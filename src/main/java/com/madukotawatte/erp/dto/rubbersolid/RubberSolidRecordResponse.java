package com.madukotawatte.erp.dto.rubbersolid;

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
public class RubberSolidRecordResponse {
    private String recordId;
    private String loadId;
    private BigDecimal massKg;
    private LocalDateTime createdAt;
}
