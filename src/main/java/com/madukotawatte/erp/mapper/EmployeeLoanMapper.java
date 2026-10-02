package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.dto.loan.LoanRepaymentResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;
import com.madukotawatte.erp.entity.EmployeeLoanRepayment;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class EmployeeLoanMapper {
    private EmployeeLoanMapper() {}

    public static EmployeeLoan toEntity(EmployeeLoanRequest request, Employee employee) {
        EmployeeLoan loan = new EmployeeLoan();
        loan.setLoanId(UUID.randomUUID().toString());
        loan.setEmployee(employee);
        loan.setPrincipalAmount(request.getPrincipalAmount());
        loan.setInterest(request.getInterest());
        loan.setInstallment(request.getInstallment());
        BigDecimal balance = request.getPrincipalAmount()
                .add(request.getPrincipalAmount().multiply(request.getInterest()).divide(BigDecimal.valueOf(100)));
        loan.setCurrentBalance(balance);
        loan.setIssuedDate(request.getIssuedDate());
        loan.setIsActive(true);
        return loan;
    }

    public static EmployeeLoanResponse toResponse(EmployeeLoan loan) {
        return toResponse(loan, List.of());
    }

    public static EmployeeLoanResponse toResponse(EmployeeLoan loan, List<EmployeeLoanRepayment> repayments) {
        BigDecimal repaid = repayments.stream().map(EmployeeLoanRepayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return EmployeeLoanResponse.builder()
                .loanId(loan.getLoanId())
                .employeeId(loan.getEmployee().getEmployeeId())
                .employeeName(loan.getEmployee().getName())
                .principalAmount(loan.getPrincipalAmount())
                .interest(loan.getInterest())
                .installment(loan.getInstallment())
                .currentBalance(loan.getCurrentBalance())
                .isActive(loan.getIsActive())
                .issuedDate(loan.getIssuedDate())
                .ledgerTransactionId(loan.getTransaction() != null ? loan.getTransaction().getId() : null)
                .totalRepaid(repaid)
                .repayments(repayments.stream().map(EmployeeLoanMapper::toRepaymentResponse).toList())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }

    public static LoanRepaymentResponse toRepaymentResponse(EmployeeLoanRepayment r) {
        MonetaryAssetTransaction tx = r.getTransaction();
        return LoanRepaymentResponse.builder()
                .repaymentId(r.getRepaymentId())
                .loanId(r.getLoan().getLoanId())
                .amount(r.getAmount())
                .repaymentDate(r.getRepaymentDate())
                .source(r.getSource())
                .payslipId(r.getPayslip() != null ? r.getPayslip().getPayslipId() : null)
                .accountCode(tx != null ? tx.getAccountCode() : null)
                .ledgerTransactionId(tx != null ? tx.getId() : null)
                .build();
    }
}
