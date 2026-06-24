package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vehicle_maintenance_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VehicleMaintenanceRecord extends BaseEntity {

    @Id
    @Column(name = "maintenance_id", length = 36)
    private String maintenanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id")
    private Expense expense;

    @Column(name = "maintenance_date", nullable = false)
    private LocalDate maintenanceDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 15, scale = 2)
    private BigDecimal cost;

    @Column(name = "service_provider", length = 255)
    private String serviceProvider;
}
