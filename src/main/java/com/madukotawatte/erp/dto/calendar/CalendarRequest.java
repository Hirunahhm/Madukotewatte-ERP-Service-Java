package com.madukotawatte.erp.dto.calendar;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CalendarRequest {
    @NotNull
    private LocalDate calendarDate;

    private Boolean isHoliday = false;

    private Boolean rained = false;

    private String description;
}
