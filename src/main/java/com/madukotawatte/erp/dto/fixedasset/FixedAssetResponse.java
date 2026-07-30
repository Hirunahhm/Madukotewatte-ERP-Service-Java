package com.madukotawatte.erp.dto.fixedasset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedAssetResponse {
    private String assetId;
    private String category;
    private String name;
    private LocalDate acquisitionDate;
    private BigDecimal acquisitionValue;
    private BigDecimal currentValue;
    private String status;
    private String location;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
