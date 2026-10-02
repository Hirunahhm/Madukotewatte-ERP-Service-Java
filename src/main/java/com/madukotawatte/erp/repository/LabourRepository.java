package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Labour;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LabourRepository extends JpaRepository<Labour, String> {
    List<Labour> findByEmployee_EmployeeId(String employeeId);
    Page<Labour> findByEmployee_EmployeeId(String employeeId, Pageable pageable);
    List<Labour> findByTimestampBetween(LocalDateTime from, LocalDateTime to);
    List<Labour> findByIsPaidAndTimestampBetween(Boolean isPaid, LocalDateTime from, LocalDateTime to);

    /** Unpaid labour of an employee in [from, to) that is not yet on a payslip. */
    @Query("select l from Labour l where l.employee.employeeId = :eid and l.isPaid = false and l.payslip is null "
            + "and l.timestamp >= :from and l.timestamp < :to order by l.timestamp")
    List<Labour> findUnpaid(@Param("eid") String employeeId, @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    List<Labour> findByPayslip_PayslipId(String payslipId);
}
