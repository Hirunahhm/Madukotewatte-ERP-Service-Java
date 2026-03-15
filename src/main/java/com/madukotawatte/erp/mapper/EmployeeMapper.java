package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.entity.Employee;

import java.util.UUID;

public class EmployeeMapper {
    private EmployeeMapper() {}

    public static Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setEmployeeId(UUID.randomUUID().toString());
        employee.setName(request.getName());
        employee.setJoinedDate(request.getJoinedDate());
        employee.setSalary(request.getSalary());
        employee.setPosition(request.getPosition());
        return employee;
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .joinedDate(employee.getJoinedDate())
                .salary(employee.getSalary())
                .position(employee.getPosition())
                .createdAt(employee.getCreatedAt())
                .updatedAt(employee.getUpdatedAt())
                .build();
    }

    public static EmployeeSummaryResponse toSummaryResponse(Employee employee) {
        return EmployeeSummaryResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .position(employee.getPosition())
                .salary(employee.getSalary())
                .build();
    }
}
