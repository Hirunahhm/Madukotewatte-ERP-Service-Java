package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Labour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabourRepository extends JpaRepository<Labour, String> {
    List<Labour> findByEmployee_EmployeeId(String employeeId);
}
