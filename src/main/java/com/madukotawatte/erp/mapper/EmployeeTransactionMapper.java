package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionRequest;
import com.madukotawatte.erp.dto.employeetransaction.EmployeeTransactionResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;

import java.util.UUID;

public class EmployeeTransactionMapper {
    private EmployeeTransactionMapper() {}

    public static EmployeeTransaction toEntity(EmployeeTransactionRequest request, Employee employee) {
        EmployeeTransaction transaction = new EmployeeTransaction();
        transaction.setTransactionRecordId(UUID.randomUUID().toString());
        transaction.setEmployee(employee);
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setTimestamp(request.getTimestamp());
        return transaction;
    }

    public static EmployeeTransactionResponse toResponse(EmployeeTransaction transaction) {
        return EmployeeTransactionResponse.builder()
                .transactionRecordId(transaction.getTransactionRecordId())
                .employeeId(transaction.getEmployee().getEmployeeId())
                .employeeName(transaction.getEmployee().getName())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .timestamp(transaction.getTimestamp())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
