package com.madukotawatte.erp.repository;

import com.madukotawatte.erp.entity.EmployeeLoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeLoanRepaymentRepository extends JpaRepository<EmployeeLoanRepayment, String> {
    List<EmployeeLoanRepayment> findByLoan_LoanIdOrderByRepaymentDateAscCreatedAtAsc(String loanId);
    List<EmployeeLoanRepayment> findByPayslip_PayslipId(String payslipId);
}
