package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordRequest;
import com.madukotawatte.erp.dto.ammonia.AmmoniaRecordResponse;
import com.madukotawatte.erp.entity.AmmoniaRecord;

import java.util.UUID;

public class AmmoniaMapper {
    private AmmoniaMapper() {}

    public static AmmoniaRecord toEntity(AmmoniaRecordRequest request) {
        AmmoniaRecord record = new AmmoniaRecord();
        record.setRecordId(UUID.randomUUID().toString());
        record.setType(request.getType());
        record.setLitres(request.getLitres());
        record.setTimestamp(request.getTimestamp());
        return record;
    }

    public static AmmoniaRecordResponse toResponse(AmmoniaRecord record) {
        return AmmoniaRecordResponse.builder()
                .recordId(record.getRecordId())
                .type(record.getType())
                .litres(record.getLitres())
                .timestamp(record.getTimestamp())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
