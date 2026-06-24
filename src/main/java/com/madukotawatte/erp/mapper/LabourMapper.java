package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.labour.LabourRequest;
import com.madukotawatte.erp.dto.labour.LabourResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.EmployeeTransaction;
import com.madukotawatte.erp.entity.Labour;

import java.util.UUID;

public class LabourMapper {
    private LabourMapper() {}

    public static Labour toEntity(LabourRequest request, Employee employee, EmployeeTransaction transaction) {
        Labour labour = new Labour();
        labour.setLabourId(UUID.randomUUID().toString());
        labour.setEmployee(employee);
        labour.setEmployeeTransaction(transaction);
        labour.setIsPaid(request.getIsPaid() != null ? request.getIsPaid() : false);
        labour.setWorkedHours(request.getWorkedHours());
        labour.setHourlyRate(request.getHourlyRate());
        labour.setAmount(request.getAmount());
        labour.setWorkType(request.getWorkType());
        labour.setDescription(request.getDescription());
        return labour;
    }

    public static LabourResponse toResponse(Labour labour) {
        return LabourResponse.builder()
                .labourId(labour.getLabourId())
                .employeeId(labour.getEmployee().getEmployeeId())
                .employeeName(labour.getEmployee().getName())
                .transactionRecordId(labour.getEmployeeTransaction() != null
                        ? labour.getEmployeeTransaction().getTransactionRecordId() : null)
                .isPaid(labour.getIsPaid())
                .workedHours(labour.getWorkedHours())
                .hourlyRate(labour.getHourlyRate())
                .amount(labour.getAmount())
                .workType(labour.getWorkType())
                .description(labour.getDescription())
                .createdAt(labour.getCreatedAt())
                .updatedAt(labour.getUpdatedAt())
                .build();
    }
}
