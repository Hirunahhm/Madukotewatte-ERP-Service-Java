package com.madukotawatte.erp.mapper;

import com.madukotawatte.erp.dto.attendance.AttendanceRequest;
import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.entity.Attendance;
import com.madukotawatte.erp.entity.Employee;

import java.util.UUID;

public class AttendanceMapper {
    private AttendanceMapper() {}

    public static Attendance toEntity(AttendanceRequest request, Employee employee) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(UUID.randomUUID().toString());
        attendance.setEmployee(employee);
        attendance.setTimestamp(request.getTimestamp());
        attendance.setNoOfTrees(request.getNoOfTrees());
        attendance.setNoWork(request.getNoWork() != null ? request.getNoWork() : "none");
        return attendance;
    }

    public static AttendanceResponse toResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .attendanceId(attendance.getAttendanceId())
                .employeeId(attendance.getEmployee().getEmployeeId())
                .employeeName(attendance.getEmployee().getName())
                .timestamp(attendance.getTimestamp())
                .noOfTrees(attendance.getNoOfTrees())
                .noWork(attendance.getNoWork())
                .createdAt(attendance.getCreatedAt())
                .build();
    }
}
