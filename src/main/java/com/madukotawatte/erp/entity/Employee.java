package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee extends BaseEntity {

    @Id
    @Column(name = "employee_id", length = 36)
    private String employeeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    private BigDecimal salary;

    private String position;
}
