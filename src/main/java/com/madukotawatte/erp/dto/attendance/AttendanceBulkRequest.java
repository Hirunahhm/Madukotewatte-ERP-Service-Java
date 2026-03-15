package com.madukotawatte.erp.dto.attendance;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AttendanceBulkRequest {
    @NotEmpty
    private List<AttendanceRequest> attendances;
}
