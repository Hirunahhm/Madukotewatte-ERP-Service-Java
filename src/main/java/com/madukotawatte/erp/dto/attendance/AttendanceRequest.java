package com.madukotawatte.erp.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AttendanceRequest {
    @NotBlank
    private String employeeId;

    @NotNull
    private LocalDateTime timestamp;

    private Integer noOfTrees;

    private String noWork;
}
