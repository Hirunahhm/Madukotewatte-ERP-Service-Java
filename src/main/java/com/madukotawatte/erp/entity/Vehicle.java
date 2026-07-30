package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle extends BaseEntity {

    @Id
    @Column(name = "vehicle_id", length = 36)
    private String vehicleId;

    @Column(name = "registration_no", nullable = false, unique = true, length = 50)
    private String registrationNo;

    @Column(length = 100)
    private String make;

    @Column(length = 100)
    private String model;

    private Integer year;

    @Column(nullable = false, length = 20)
    private String status = "active";
}
