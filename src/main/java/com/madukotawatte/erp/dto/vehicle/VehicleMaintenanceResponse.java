package com.madukotawatte.erp.dto.vehicle;

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
public class VehicleMaintenanceResponse {
    private String maintenanceId;
    private String vehicleId;
    private String registrationNo;
    private String expenseId;
    private LocalDate maintenanceDate;
    private String description;
    private BigDecimal cost;
    private String serviceProvider;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
