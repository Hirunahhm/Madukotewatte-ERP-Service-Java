package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.employee.EmployeeRequest;
import com.madukotawatte.erp.dto.employee.EmployeeResponse;
import com.madukotawatte.erp.dto.employee.EmployeeSummaryResponse;
import com.madukotawatte.erp.entity.Employee;

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
        employee.setRatePerBunch(request.getRatePerBunch() != null ? request.getRatePerBunch() : BigDecimal.ZERO);
        employee.setRatePerNut(request.getRatePerNut() != null ? request.getRatePerNut() : BigDecimal.ZERO);
        employee.setRatePerKgManioc(request.getRatePerKgManioc() != null ? request.getRatePerKgManioc() : BigDecimal.ZERO);
        employee.setPosition(request.getPosition());
        return employee;
    }

    public static EmployeeResponse toResponse(Employee employee) {
        return EmployeeResponse.builder()
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .joinedDate(employee.getJoinedDate())
                .salary(employee.getSalary())
                .ratePerTree(employee.getRatePerTree())
                .ratePerBunch(employee.getRatePerBunch())
                .ratePerNut(employee.getRatePerNut())
                .ratePerKgManioc(employee.getRatePerKgManioc())
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
                .isActive(employee.getIsActive())
                .build();
    }
}
