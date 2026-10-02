package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EmployeeTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmployeeTransactionRepository extends JpaRepository<EmployeeTransaction, String>, JpaSpecificationExecutor<EmployeeTransaction> {
    List<EmployeeTransaction> findByEmployee_EmployeeId(String employeeId);
    Page<EmployeeTransaction> findByEmployee_EmployeeIdAndTimestampBetween(String employeeId, LocalDateTime from, LocalDateTime to, Pageable pageable);
    List<EmployeeTransaction> findByEmployee_EmployeeIdAndType(String employeeId, String type);

    /** Latex_Tap earnings of an employee in [from, to) that are not yet on a payslip. */
    @Query("select t from EmployeeTransaction t where t.employee.employeeId = :eid and t.type = 'Latex_Tap' "
            + "and t.payslip is null and t.timestamp >= :from and t.timestamp < :to order by t.timestamp")
    List<EmployeeTransaction> findUnpaidTaps(@Param("eid") String employeeId, @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    List<EmployeeTransaction> findByPayslip_PayslipId(String payslipId);

    @Query("select coalesce(sum(t.amount), 0) from EmployeeTransaction t "
            + "where t.employee.employeeId = :eid and t.type = :type")
    BigDecimal sumByEmployeeAndType(@Param("eid") String employeeId, @Param("type") String type);

    /** [employeeId, type, sum] for every employee, for the given types. */
    @Query("select t.employee.employeeId, t.type, coalesce(sum(t.amount), 0) from EmployeeTransaction t "
            + "where t.type in :types group by t.employee.employeeId, t.type")
    List<Object[]> sumByTypes(@Param("types") List<String> types);
}
