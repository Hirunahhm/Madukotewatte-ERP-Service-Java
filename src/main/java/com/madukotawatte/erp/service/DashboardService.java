package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.dashboard.*;
import com.madukotawatte.erp.dto.payroll.PayrollPreviewRow;
import com.madukotawatte.erp.dto.payroll.PayrollSummaryRow;
import com.madukotawatte.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final AttendanceRepository attendanceRepository;
    private final PayrollService payrollService;

    /**
     * Month view built on the payroll service: for every ACTIVE employee what is still payable for the month
     * (the payroll preview: unpaid fixed salary, tapping, hourly labour, outstanding advance, suggested
     * deductions, net) next to what has already been paid on payslips with a payment date in that month.
     */
    public MonthlyPayrollResponse getMonthlyPayroll(int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to = end.plusDays(1).atStartOfDay();

        Map<String, PayrollSummaryRow> paid = new HashMap<>();
        for (PayrollSummaryRow row : payrollService.summary(start, end).getRows()) {
            paid.put(row.getEmployeeId(), row);
        }
        List<EmployeePayrollEntry> entries = new ArrayList<>();
        for (PayrollPreviewRow row : payrollService.preview(start, end, null)) {
            int workDays = (int) attendanceRepository
                    .findByEmployee_EmployeeIdAndTimestampBetween(row.getEmployeeId(), from, to).stream()
                    .filter(a -> "none".equals(a.getNoWork()))
                    .count();
            PayrollSummaryRow done = paid.get(row.getEmployeeId());
            entries.add(EmployeePayrollEntry.builder()
                    .employeeId(row.getEmployeeId())
                    .employeeName(row.getEmployeeName())
                    .salary(row.getFixedSalary())
                    .workDays(workDays)
                    .latexTap(row.getTapEarnings())
                    .advances(row.getOutstandingAdvances())
                    .loanDeductions(row.getSuggestedLoanDeduction())
                    .manualLabor(row.getHourlyEarnings())
                    .net(row.getNetPay())
                    .paidGross(done != null ? done.getGross() : BigDecimal.ZERO)
                    .paidNet(done != null ? done.getNet() : BigDecimal.ZERO)
                    .payslipCount(done != null ? done.getPayslipCount() : 0)
                    .build());
        }
        return MonthlyPayrollResponse.builder().month(month).year(year).entries(entries).build();
    }

    public byte[] exportPayroll(int month, int year) {
        MonthlyPayrollResponse payroll = getMonthlyPayroll(month, year);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Payroll " + month + "-" + year);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Name", "Fixed Salary (unpaid)", "Work Days", "Tapping (unpaid)",
                    "Hourly Labour (unpaid)", "Outstanding Advances", "Suggested Loan Deduction", "Net Payable",
                    "Paid Gross (payslips)", "Paid Net (payslips)", "Payslips"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (EmployeePayrollEntry entry : payroll.getEntries()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getEmployeeId());
                row.createCell(1).setCellValue(entry.getEmployeeName());
                row.createCell(2).setCellValue(entry.getSalary().doubleValue());
                row.createCell(3).setCellValue(entry.getWorkDays());
                row.createCell(4).setCellValue(entry.getLatexTap().doubleValue());
                row.createCell(5).setCellValue(entry.getManualLabor().doubleValue());
                row.createCell(6).setCellValue(entry.getAdvances().doubleValue());
                row.createCell(7).setCellValue(entry.getLoanDeductions().doubleValue());
                row.createCell(8).setCellValue(entry.getNet().doubleValue());
                row.createCell(9).setCellValue(entry.getPaidGross().doubleValue());
                row.createCell(10).setCellValue(entry.getPaidNet().doubleValue());
                row.createCell(11).setCellValue(entry.getPayslipCount());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate payroll export", e);
        }
    }
}
