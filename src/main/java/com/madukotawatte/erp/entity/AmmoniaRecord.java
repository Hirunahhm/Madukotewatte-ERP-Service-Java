package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ammonia_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AmmoniaRecord extends BaseEntity {

    @Id
    @Column(name = "record_id", length = 36)
    private String recordId;

    // 'Refill' | 'Out'
    @Column(length = 10)
    private String type;

    @Column(precision = 10, scale = 2)
    private BigDecimal litres;

    private LocalDateTime timestamp;
}
