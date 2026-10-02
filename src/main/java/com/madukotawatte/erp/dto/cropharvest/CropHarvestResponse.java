package com.madukotawatte.erp.dto.cropharvest;

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
public class CropHarvestResponse {
    private String id;
    private String crop;
    private LocalDate harvestDate;
    private BigDecimal quantity;
    private String unit;
    private String fieldBlock;
    private String employeeId;
    private String employeeName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
