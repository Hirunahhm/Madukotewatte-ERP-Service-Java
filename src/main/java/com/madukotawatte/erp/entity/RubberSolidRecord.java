package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "rubber_solid_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RubberSolidRecord extends BaseEntity {

    @Id
    @Column(name = "record_id", length = 36)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id")
    private Load load;

    @Column(name = "mass_kg", precision = 10, scale = 2)
    private BigDecimal massKg;
}
