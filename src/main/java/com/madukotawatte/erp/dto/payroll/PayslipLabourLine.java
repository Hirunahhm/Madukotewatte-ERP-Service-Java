package com.madukotawatte.erp.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayslipLabourLine {
    private String labourId;
    private LocalDate date;
    private String workType;
    private BigDecimal workedHours;
    private BigDecimal hourlyRate;
    private BigDecimal amount;
}
