package com.madukotawatte.erp.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VehicleRequest {
    @NotBlank
    private String registrationNo;

    private String make;

    private String model;

    private Integer year;

    private String status = "active";
}
