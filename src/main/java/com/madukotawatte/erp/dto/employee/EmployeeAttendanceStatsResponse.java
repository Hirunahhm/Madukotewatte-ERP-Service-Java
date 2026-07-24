package com.madukotawatte.erp.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendanceStatsResponse {
    private long totalDays;
    private long presentDays;
    private long absentDays;
    private long totalTreesTapped;
    private double avgTreesPerPresentDay;
    private double attendanceRatePercent;
}
