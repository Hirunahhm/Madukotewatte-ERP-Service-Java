package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.entity.EmployeeTransaction;

public class EmployeeTransactionMapper {
    private EmployeeTransactionMapper() {}

    public static EmployeeTransactionResponse toResponse(EmployeeTransaction transaction) {
        return EmployeeTransactionResponse.builder()
                .transactionRecordId(transaction.getTransactionRecordId())
                .employeeId(transaction.getEmployee().getEmployeeId())
                .employeeName(transaction.getEmployee().getName())
                .type(transaction.getType())
                .accountCode(transaction.getAccountCode())
                .amount(transaction.getAmount())
                .timestamp(transaction.getTimestamp())
                .ledgerTransactionId(transaction.getTransaction() != null ? transaction.getTransaction().getId() : null)
                .note(transaction.getNote())
                .payslipId(transaction.getPayslip() != null ? transaction.getPayslip().getPayslipId() : null)
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
