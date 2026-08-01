package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coconut_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CoconutRecord extends BaseEntity {

    @Id
    @Column(name = "record_id", length = 36)
    private String recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id")
    private Load load;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_record_id")
    private EmployeeTransaction employeeTransaction;

    // 'Sri Lanka Tall' | 'King Coconut (Thembili)' | 'Green Dwarf' | 'Yellow Dwarf' | 'CRIC-65' | 'San Ramon' | 'Other'
    @Column(length = 30)
    private String variety;

    @Column(name = "variety_note", length = 50)
    private String varietyNote;

    @Column(name = "nut_count")
    private Integer nutCount;

    @Column(name = "mass_kg", precision = 10, scale = 2)
    private BigDecimal massKg;

    private LocalDateTime timestamp;
}
