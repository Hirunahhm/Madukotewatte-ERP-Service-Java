package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, String> {
    Page<Attendance> findByEmployee_EmployeeId(String employeeId, Pageable pageable);
    List<Attendance> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<Attendance> findByEmployee_EmployeeIdAndTimestampBetween(String employeeId, LocalDateTime from, LocalDateTime to);
}
