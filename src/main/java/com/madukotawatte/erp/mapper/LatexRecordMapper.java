package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.latex.LatexRecordRequest;
import com.madukotawatte.erp.dto.latex.LatexRecordResponse;
import com.madukotawatte.erp.entity.Employee;
import com.madukotawatte.erp.entity.LatexRecord;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.MetrolacReading;

import java.util.UUID;

public class LatexRecordMapper {
    private LatexRecordMapper() {}

    public static LatexRecord toEntity(LatexRecordRequest request, Load load, Employee employee, MetrolacReading metrolacReading) {
        LatexRecord record = new LatexRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setLoad(load);
        record.setEmployee(employee);
        record.setTimestamp(request.getTimestamp());
        record.setLatexAmount(request.getLatexAmount());
        record.setAmmoniaAmount(request.getAmmoniaAmount());
        record.setMetrolacReading(metrolacReading);
        return record;
    }

    public static LatexRecordResponse toResponse(LatexRecord record) {
        return LatexRecordResponse.builder()
                .recordId(record.getRecordId())
                .loadId(record.getLoad().getLoadId())
                .employeeId(record.getEmployee().getEmployeeId())
                .employeeName(record.getEmployee().getName())
                .timestamp(record.getTimestamp())
                .latexAmount(record.getLatexAmount())
                .ammoniaAmount(record.getAmmoniaAmount())
                .metrolacReadingId(record.getMetrolacReading() != null ? record.getMetrolacReading().getMetrolacId() : null)
                .createdAt(record.getCreatedAt())
                .build();
    }
}
