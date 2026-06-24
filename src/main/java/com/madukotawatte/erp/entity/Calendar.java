package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "calendar")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Calendar extends BaseEntity {

    @Id
    @Column(name = "calendar_id", length = 36)
    private String calendarId;

    @Column(name = "calendar_date", nullable = false, unique = true)
    private LocalDate calendarDate;

    @Column(name = "is_holiday", nullable = false)
    private Boolean isHoliday = false;

    @Column(nullable = false)
    private Boolean rained = false;

    @Column(length = 255)
    private String description;
}
