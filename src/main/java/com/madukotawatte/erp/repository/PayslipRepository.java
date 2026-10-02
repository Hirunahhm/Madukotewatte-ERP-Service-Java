package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.Payslip;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PayslipRepository extends JpaRepository<Payslip, String> {

    /** PAID payslips of an employee whose period overlaps [from, to]. */
    @Query("select p from Payslip p where p.employee.employeeId = :eid and p.status = 'PAID' "
            + "and p.periodStart <= :to and p.periodEnd >= :from order by p.periodStart")
    List<Payslip> findPaidOverlapping(@Param("eid") String employeeId, @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    /** PAID payslips (all employees) whose period overlaps [from, to]. */
    @Query("select p from Payslip p where p.status = 'PAID' and p.periodStart <= :to and p.periodEnd >= :from")
    List<Payslip> findAllPaidOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select p from Payslip p where p.status = 'PAID' and p.paymentDate >= :from and p.paymentDate <= :to")
    List<Payslip> findPaidByPaymentDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select p from Payslip p where (:eid is null or p.employee.employeeId = :eid) "
            + "and (:from is null or p.paymentDate >= :from) and (:to is null or p.paymentDate <= :to) "
            + "order by p.paymentDate desc, p.createdAt desc")
    Page<Payslip> search(@Param("eid") String employeeId, @Param("from") LocalDate from,
                         @Param("to") LocalDate to, Pageable pageable);
}
