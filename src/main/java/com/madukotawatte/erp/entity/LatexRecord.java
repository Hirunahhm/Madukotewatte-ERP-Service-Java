package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "latex_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LatexRecord extends BaseEntity {

    @Id
    @Column(name = "record_id", length = 36)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id")
    private Load load;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    private LocalDateTime timestamp;

    @Column(name = "latex_amount", precision = 10, scale = 2)
    private BigDecimal latexAmount;

    @Column(name = "ammonia_amount", precision = 10, scale = 2)
    private BigDecimal ammoniaAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metrolac_id")
    private MetrolacReading metrolacReading;
}
