package com.madukotawatte.erp.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {
    private String vehicleId;
    private String registrationNo;
    private String make;
    private String model;
    private Integer year;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
