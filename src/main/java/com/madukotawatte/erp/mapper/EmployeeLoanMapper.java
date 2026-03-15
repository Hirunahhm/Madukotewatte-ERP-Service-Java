package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.loan.EmployeeLoanRequest;
import com.madukotawatte.erp.dto.loan.EmployeeLoanResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeLoan;

import java.math.BigDecimal;
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
        loan.setIsActive(true);
        return loan;
    }

    public static EmployeeLoanResponse toResponse(EmployeeLoan loan) {
        return EmployeeLoanResponse.builder()
                .loanId(loan.getLoanId())
                .employeeId(loan.getEmployee().getEmployeeId())
                .employeeName(loan.getEmployee().getName())
                .principalAmount(loan.getPrincipalAmount())
                .interest(loan.getInterest())
                .installment(loan.getInstallment())
                .currentBalance(loan.getCurrentBalance())
                .isActive(loan.getIsActive())
                .createdAt(loan.getCreatedAt())
                .updatedAt(loan.getUpdatedAt())
                .build();
    }
}
