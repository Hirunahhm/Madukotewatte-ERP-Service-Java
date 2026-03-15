package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "metrolac_readings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MetrolacReading extends BaseEntity {

    @Id
    @Column(name = "metrolac_id", length = 36)
    private String metrolacId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id")
    private Load load;

    @Column(precision = 5, scale = 2)
    private BigDecimal temperature;

    private LocalDateTime timestamp;
}
