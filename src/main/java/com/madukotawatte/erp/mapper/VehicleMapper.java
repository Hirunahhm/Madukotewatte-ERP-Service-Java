package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.vehicle.VehicleMaintenanceRequest;
import com.madukotawatte.erp.dto.vehicle.VehicleMaintenanceResponse;
import com.madukotawatte.erp.dto.vehicle.VehicleRequest;
import com.madukotawatte.erp.dto.vehicle.VehicleResponse;
import com.madukotawatte.erp.entity.Expense;
import com.madukotawatte.erp.entity.Vehicle;
import com.madukotawatte.erp.entity.VehicleMaintenanceRecord;

import java.util.UUID;

public class VehicleMapper {
    private VehicleMapper() {}

    public static Vehicle toEntity(VehicleRequest request) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(UUID.randomUUID().toString());
        vehicle.setRegistrationNo(request.getRegistrationNo());
        vehicle.setMake(request.getMake());
        vehicle.setModel(request.getModel());
        vehicle.setYear(request.getYear());
        vehicle.setStatus(request.getStatus() != null ? request.getStatus() : "active");
        return vehicle;
    }

    public static VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .vehicleId(vehicle.getVehicleId())
                .registrationNo(vehicle.getRegistrationNo())
                .make(vehicle.getMake())
                .model(vehicle.getModel())
                .year(vehicle.getYear())
                .status(vehicle.getStatus())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    public static VehicleMaintenanceRecord toMaintenanceEntity(
            VehicleMaintenanceRequest request, Vehicle vehicle, Expense expense) {
        VehicleMaintenanceRecord record = new VehicleMaintenanceRecord();
        record.setMaintenanceId(UUID.randomUUID().toString());
        record.setVehicle(vehicle);
        record.setExpense(expense);
        record.setMaintenanceDate(request.getMaintenanceDate());
        record.setDescription(request.getDescription());
        record.setCost(request.getCost());
        record.setServiceProvider(request.getServiceProvider());
        return record;
    }

    public static VehicleMaintenanceResponse toMaintenanceResponse(VehicleMaintenanceRecord record) {
        return VehicleMaintenanceResponse.builder()
                .maintenanceId(record.getMaintenanceId())
                .vehicleId(record.getVehicle().getVehicleId())
                .registrationNo(record.getVehicle().getRegistrationNo())
                .expenseId(record.getExpense() != null ? record.getExpense().getExpenseId() : null)
                .maintenanceDate(record.getMaintenanceDate())
                .description(record.getDescription())
                .cost(record.getCost())
                .serviceProvider(record.getServiceProvider())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
