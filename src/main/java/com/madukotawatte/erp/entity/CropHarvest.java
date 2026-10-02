package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A harvest of cocoa or another minor crop (pepper, cinnamon, coffee, arecanut, cloves, ...). */
@Entity
@Table(name = "crop_harvests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CropHarvest extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 30)
    private String crop;

    @Column(name = "harvest_date", nullable = false)
    private LocalDate harvestDate;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "field_block", length = 100)
    private String fieldBlock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(length = 500)
    private String notes;
}
