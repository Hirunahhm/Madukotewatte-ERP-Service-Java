package com.madukotawatte.erp.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyPayrollResponse {
    private int month;
    private int year;
    private List<EmployeePayrollEntry> entries;
}
