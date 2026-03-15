package com.madukotawatte.erp.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private String attendanceId;
    private String employeeId;
    private String employeeName;
    private LocalDateTime timestamp;
    private Integer noOfTrees;
    private String noWork;
    private LocalDateTime createdAt;
}
