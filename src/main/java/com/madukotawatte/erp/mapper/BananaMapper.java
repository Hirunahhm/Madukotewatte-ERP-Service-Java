package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.banana.BananaRecordRequest;
import com.madukotawatte.erp.dto.banana.BananaRecordResponse;
import com.madukotawatte.erp.entity.BananaRecord;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.Load;

import java.util.UUID;

public class BananaMapper {
    private BananaMapper() {}

    public static BananaRecord toEntity(BananaRecordRequest request, Load load, Employee employee) {
        BananaRecord record = new BananaRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setVariety(request.getVariety());
        record.setVarietyNote(request.getVarietyNote());
        record.setBunchCount(request.getBunchCount());
        record.setMassKg(request.getMassKg());
        record.setTimestamp(request.getTimestamp());
        return record;
    }

    public static BananaRecordResponse toResponse(BananaRecord record) {
        return BananaRecordResponse.builder()
                .recordId(record.getRecordId())
                .loadId(record.getLoad().getLoadId())
                .employeeId(record.getEmployee() != null ? record.getEmployee().getEmployeeId() : null)
                .employeeName(record.getEmployee() != null ? record.getEmployee().getName() : null)
                .variety(record.getVariety())
                .varietyNote(record.getVarietyNote())
                .bunchCount(record.getBunchCount())
                .massKg(record.getMassKg())
                .timestamp(record.getTimestamp())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
