package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.manioc.ManiocRecordRequest;
import com.madukotawatte.erp.dto.manioc.ManiocRecordResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.ManiocRecord;

import java.util.UUID;

public class ManiocMapper {
    private ManiocMapper() {}

    public static ManiocRecord toEntity(ManiocRecordRequest request, Load load, Employee employee) {
        ManiocRecord record = new ManiocRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setVariety(request.getVariety());
        record.setVarietyNote(request.getVarietyNote());
        record.setMassKg(request.getMassKg());
        record.setTimestamp(request.getTimestamp());
        return record;
    }

    public static ManiocRecordResponse toResponse(ManiocRecord record) {
        return ManiocRecordResponse.builder()
                .recordId(record.getRecordId())
                .loadId(record.getLoad().getLoadId())
                .employeeId(record.getEmployee() != null ? record.getEmployee().getEmployeeId() : null)
                .employeeName(record.getEmployee() != null ? record.getEmployee().getName() : null)
                .variety(record.getVariety())
                .varietyNote(record.getVarietyNote())
                .massKg(record.getMassKg())
                .timestamp(record.getTimestamp())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
