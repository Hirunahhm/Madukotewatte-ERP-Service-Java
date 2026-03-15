package com.madukotawatte.erp.dto.dashboard;

import com.madukotawatte.erp.dto.attendance.AttendanceResponse;
import com.madukotawatte.erp.dto.expense.ExpenseResponse;
import com.madukotawatte.erp.dto.latex.LatexRecordResponse;
import com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyReportResponse {
    private LocalDate date;
    private List<AttendanceResponse> attendances;
    private List<LatexRecordResponse> latexRecords;
    private List<ExpenseResponse> expenses;
    private List<RubberSolidRecordResponse> rubberSolidRecords;
}
