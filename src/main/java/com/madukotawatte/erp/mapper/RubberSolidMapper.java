package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordRequest;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.RubberSolidRecord;

import java.util.UUID;

public class RubberSolidMapper {
    private RubberSolidMapper() {}

    public static RubberSolidRecord toEntity(RubberSolidRecordRequest request, Load load) {
        RubberSolidRecord record = new RubberSolidRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setLoad(load);
        record.setMassKg(request.getMassKg());
        return record;
    }

    public static RubberSolidRecordResponse toResponse(RubberSolidRecord record) {
        return RubberSolidRecordResponse.builder()
                .recordId(record.getRecordId())
                .loadId(record.getLoad().getLoadId())
                .massKg(record.getMassKg())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
