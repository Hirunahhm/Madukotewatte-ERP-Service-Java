package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "load_table")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Load extends BaseEntity {

    @Id
    @Column(name = "load_id", length = 36)
    private String loadId;

    @Column(name = "load_type", length = 50)
    private String loadType;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;
}
