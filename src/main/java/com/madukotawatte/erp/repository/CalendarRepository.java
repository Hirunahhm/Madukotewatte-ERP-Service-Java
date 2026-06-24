package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Calendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CalendarRepository extends JpaRepository<Calendar, String> {
    Optional<Calendar> findByCalendarDate(LocalDate calendarDate);
}
