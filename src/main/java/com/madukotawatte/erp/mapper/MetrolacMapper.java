package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.metrolac.MetrolacReadingRequest;
import com.madukotawatte.erp.dto.metrolac.MetrolacReadingResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.MetrolacReading;

import java.util.UUID;

public class MetrolacMapper {
    private MetrolacMapper() {}

    public static MetrolacReading toEntity(MetrolacReadingRequest request, Load load) {
        MetrolacReading reading = new MetrolacReading();
        reading.setMetrolacId(UUID.randomUUID().toString());
        reading.setLoad(load);
        reading.setTemperature(request.getTemperature());
        reading.setTimestamp(request.getTimestamp());
        return reading;
    }

    public static MetrolacReadingResponse toResponse(MetrolacReading reading) {
        return MetrolacReadingResponse.builder()
                .metrolacId(reading.getMetrolacId())
                .loadId(reading.getLoad().getLoadId())
                .temperature(reading.getTemperature())
                .timestamp(reading.getTimestamp())
                .createdAt(reading.getCreatedAt())
                .build();
    }
}
