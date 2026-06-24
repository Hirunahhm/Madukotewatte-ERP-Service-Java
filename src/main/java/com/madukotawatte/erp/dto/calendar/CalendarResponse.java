package com.madukotawatte.erp.dto.calendar;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarResponse {
    private String calendarId;
    private LocalDate calendarDate;
    private Boolean isHoliday;
    private Boolean rained;
    private String description;
    private LocalDateTime createdAt;
}
