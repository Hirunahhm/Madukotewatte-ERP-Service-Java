package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EmployeeTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeTransactionRepository extends JpaRepository<EmployeeTransaction, String> {
    List<EmployeeTransaction> findByEmployee_EmployeeId(String employeeId);
    Page<EmployeeTransaction> findByEmployee_EmployeeIdAndTimestampBetween(String employeeId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<EmployeeTransaction> findByEmployee_EmployeeIdAndType(String employeeId, String type);
}
