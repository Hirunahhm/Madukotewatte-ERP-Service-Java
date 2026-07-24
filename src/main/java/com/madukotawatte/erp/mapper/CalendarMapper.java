package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.calendar.CalendarRequest;
import com.madukotawatte.erp.dto.calendar.CalendarResponse;
import com.madukotawatte.erp.entity.Calendar;

import java.util.UUID;

public class CalendarMapper {
    private CalendarMapper() {}

    public static Calendar toEntity(CalendarRequest request) {
        Calendar calendar = new Calendar();
        calendar.setCalendarId(UUID.randomUUID().toString());
        calendar.setCalendarDate(request.getCalendarDate());
        calendar.setIsHoliday(request.getIsHoliday() != null ? request.getIsHoliday() : false);
        calendar.setRained(request.getRained() != null ? request.getRained() : false);
        calendar.setIsWorking(request.getIsWorking() != null ? request.getIsWorking() : true);
        calendar.setDescription(request.getDescription());
        return calendar;
    }

    public static CalendarResponse toResponse(Calendar calendar) {
        return CalendarResponse.builder()
                .calendarId(calendar.getCalendarId())
                .calendarDate(calendar.getCalendarDate())
                .isHoliday(calendar.getIsHoliday())
                .rained(calendar.getRained())
                .isWorking(calendar.getIsWorking())
                .description(calendar.getDescription())
                .createdAt(calendar.getCreatedAt())
                .build();
    }
}
