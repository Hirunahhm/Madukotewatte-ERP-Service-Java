package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.load.LoadRequest;
import com.madukotawatte.erp.dto.load.LoadResponse;
import com.madukotawatte.erp.entity.Load;

import java.util.UUID;

public class LoadMapper {
    private LoadMapper() {}

    public static Load toEntity(LoadRequest request) {
        Load load = new Load();
        load.setLoadId(UUID.randomUUID().toString());
        load.setLoadType(request.getLoadType());
        load.setStartDate(request.getStartDate());
        load.setEndDate(request.getEndDate());
        load.setStatus(request.getStatus() != null ? request.getStatus() : "pending");
        return load;
    }

    public static LoadResponse toResponse(Load load) {
        return LoadResponse.builder()
                .loadId(load.getLoadId())
                .loadType(load.getLoadType())
                .startDate(load.getStartDate())
                .endDate(load.getEndDate())
                .status(load.getStatus())
                .createdAt(load.getCreatedAt())
                .updatedAt(load.getUpdatedAt())
                .build();
    }
}
