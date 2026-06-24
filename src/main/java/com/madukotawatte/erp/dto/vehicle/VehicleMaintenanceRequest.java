package com.madukotawatte.erp.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VehicleMaintenanceRequest {
    @NotBlank
    private String vehicleId;

    private String expenseId;

    @NotNull
    private LocalDate maintenanceDate;

    private String description;

    private BigDecimal cost;

    private String serviceProvider;
}
