package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.PayType;

import java.math.BigDecimal;
import java.util.UUID;

public class EmployeeMapper {
    private EmployeeMapper() {}

    public static Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setEmployeeId(UUID.randomUUID().toString());
        employee.setName(request.getName());
        employee.setJoinedDate(request.getJoinedDate());
        employee.setSalary(request.getSalary());
        employee.setRatePerTree(request.getRatePerTree() != null ? request.getRatePerTree() : BigDecimal.ZERO);
        employee.setPosition(request.getPosition());
        employee.setPayType(resolvePayType(request));
        employee.setHourlyRate(request.getHourlyRate());
        return employee;
    }

    /** Explicit pay type, else derived from the position ("Tapper" -> PER_TREE, anything else -> FIXED_MONTHLY). */
    public static PayType resolvePayType(EmployeeRequest request) {
        if (request.getPayType() != null) return request.getPayType();
        String position = request.getPosition();
        return position != null && position.toLowerCase().contains("tapper") ? PayType.PER_TREE : PayType.FIXED_MONTHLY;
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .joinedDate(employee.getJoinedDate())
                .salary(employee.getSalary())
                .ratePerTree(employee.getRatePerTree())
                .payType(employee.getPayType())
                .hourlyRate(employee.getHourlyRate())
                .position(employee.getPosition())
                .isActive(employee.getIsActive())
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
                .payType(employee.getPayType())
                .ratePerTree(employee.getRatePerTree())
                .hourlyRate(employee.getHourlyRate())
                .isActive(employee.getIsActive())
                .build();
    }
}
