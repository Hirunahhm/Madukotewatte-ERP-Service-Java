package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.LatexRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LatexRecordRepository extends JpaRepository<LatexRecord, String> {
    List<LatexRecord> findByLoad_LoadId(String loadId);
    List<LatexRecord> findByEmployee_EmployeeId(String employeeId);
    List<LatexRecord> findByEmployee_EmployeeIdAndTimestampBetween(String employeeId, LocalDateTime from, LocalDateTime to);
    List<LatexRecord> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
}
