package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Labour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LabourRepository extends JpaRepository<Labour, String> {
    List<Labour> findByEmployee_EmployeeId(String employeeId);
    Page<Labour> findByEmployee_EmployeeId(String employeeId, Pageable pageable);
    List<Labour> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<Labour> findByIsPaidAndTimestampBetween(Boolean isPaid, LocalDateTime from, LocalDateTime to);
}
