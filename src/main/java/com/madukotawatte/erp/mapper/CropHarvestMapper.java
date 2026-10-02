package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.cropharvest.CropHarvestRequest;
import com.madukotawatte.erp.dto.cropharvest.CropHarvestResponse;
import com.madukotawatte.erp.entity.CropHarvest;
import com.madukotawatte.erp.entity.Employee;

import java.util.UUID;

public class CropHarvestMapper {
    private CropHarvestMapper() {}

    public static CropHarvest toEntity(CropHarvestRequest request, Employee employee) {
        CropHarvest harvest = new CropHarvest();
        harvest.setId(UUID.randomUUID().toString());
        applyFields(harvest, request, employee);
        return harvest;
    }

    public static void applyFields(CropHarvest harvest, CropHarvestRequest request, Employee employee) {
        harvest.setCrop(request.getCrop());
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setQuantity(request.getQuantity());
        harvest.setUnit(request.getUnit());
        harvest.setFieldBlock(request.getFieldBlock() == null || request.getFieldBlock().isBlank() ? null : request.getFieldBlock().trim());
        harvest.setEmployee(employee);
        harvest.setNotes(request.getNotes() == null || request.getNotes().isBlank() ? null : request.getNotes().trim());
    }

    public static CropHarvestResponse toResponse(CropHarvest harvest) {
        Employee employee = harvest.getEmployee();
        return CropHarvestResponse.builder()
                .id(harvest.getId())
                .crop(harvest.getCrop())
                .harvestDate(harvest.getHarvestDate())
                .quantity(harvest.getQuantity())
                .unit(harvest.getUnit())
                .fieldBlock(harvest.getFieldBlock())
                .employeeId(employee != null ? employee.getEmployeeId() : null)
                .employeeName(employee != null ? employee.getName() : null)
                .notes(harvest.getNotes())
                .createdAt(harvest.getCreatedAt())
                .updatedAt(harvest.getUpdatedAt())
                .build();
    }
}
