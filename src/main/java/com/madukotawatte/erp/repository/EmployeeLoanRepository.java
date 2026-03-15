package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EmployeeLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, String> {
    List<EmployeeLoan> findByEmployee_EmployeeId(String employeeId);
    List<EmployeeLoan> findByEmployee_EmployeeIdAndIsActiveTrue(String employeeId);
    List<EmployeeLoan> findByIsActiveTrue();
}
