package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.coconut.CoconutRecordRequest;
import com.madukotawatte.erp.dto.coconut.CoconutRecordResponse;
import com.madukotawatte.erp.entity.CoconutRecord;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.Load;

import java.util.UUID;

public class CoconutMapper {
    private CoconutMapper() {}

    public static CoconutRecord toEntity(CoconutRecordRequest request, Load load, Employee employee) {
        CoconutRecord record = new CoconutRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setVariety(request.getVariety());
        record.setVarietyNote(request.getVarietyNote());
        record.setNutCount(request.getNutCount());
        record.setMassKg(request.getMassKg());
        record.setTimestamp(request.getTimestamp());
        return record;
    }

    public static CoconutRecordResponse toResponse(CoconutRecord record) {
        return CoconutRecordResponse.builder()
                .recordId(record.getRecordId())
                .loadId(record.getLoad().getLoadId())
                .employeeId(record.getEmployee() != null ? record.getEmployee().getEmployeeId() : null)
                .employeeName(record.getEmployee() != null ? record.getEmployee().getName() : null)
                .variety(record.getVariety())
                .varietyNote(record.getVarietyNote())
                .nutCount(record.getNutCount())
                .massKg(record.getMassKg())
                .timestamp(record.getTimestamp())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
