package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.dashboard.*;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import com.madukotawatte.erp.dto.monetary.AssetBalanceResponse;
import com.madukotawatte.erp.entity.*;
import com.madukotawatte.erp.mapper.*;
import com.madukotawatte.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LatexRecordRepository latexRecordRepository;
    private final ExpenseRepository expenseRepository;
    private final RubberSolidRecordRepository rubberSolidRecordRepository;
    private final LoadRepository loadRepository;
    private final SalesLatexRepository salesLatexRepository;
    private final EmployeeLoanRepository employeeLoanRepository;
    private final EmployeeTransactionRepository employeeTransactionRepository;
    private final MonetaryAssetTransactionRepository monetaryAssetTransactionRepository;
    private final EstateLoanTransactionRepository estateLoanTransactionRepository;

    public DashboardSummaryResponse getSummary() {
        // Asset balances
        List<AssetBalanceResponse> assetBalances = monetaryAssetTransactionRepository.findBalanceByAssetType()
                .stream()
                .map(row -> AssetBalanceResponse.builder()
                        .assetType((String) row[0])
                        .balance((BigDecimal) row[1])
                        .build())
                .collect(Collectors.toList());

        // Loan balances
        List<String> loanTypes = List.of("credit-card - Peoples", "credit-card - Sampath", "Loan-mom", "Loan-other");
        List<EstateLoanBalanceResponse> loanBalances = loanTypes.stream()
                .map(loanType -> {
                    List<EstateLoanTransaction> transactions = estateLoanTransactionRepository.findByLoanType(loanType);
                    BigDecimal balance = transactions.isEmpty() ? BigDecimal.ZERO
                            : transactions.get(transactions.size() - 1).getNewAmount();
                    return EstateLoanBalanceResponse.builder()
                            .loanType(loanType)
                            .balance(balance)
                            .build();
                })
                .collect(Collectors.toList());

        // Active employee loans count
        long activeEmployeeLoans = employeeLoanRepository.findByIsActiveTrue().size();

        // Today's attendance count
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        long todayAttendanceCount = attendanceRepository.findByTimestampBetween(startOfDay, endOfDay).size();

        // Unpaid sales count
        long unpaidSalesCount = salesLatexRepository.findByIsPaymentReceivedFalse().size();

        return DashboardSummaryResponse.builder()
                .assetBalances(assetBalances)
                .loanBalances(loanBalances)
                .activeEmployeeLoans(activeEmployeeLoans)
                .todayAttendanceCount(todayAttendanceCount)
                .unpaidSalesCount(unpaidSalesCount)
                .build();
    }

    public DailyReportResponse getDailyReport(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<com.madukotawatte.erp.dto.attendance.AttendanceResponse> attendances =
                attendanceRepository.findByTimestampBetween(startOfDay, endOfDay)
                        .stream().map(AttendanceMapper::toResponse).collect(Collectors.toList());

        List<com.madukotawatte.erp.dto.latex.LatexRecordResponse> latexRecords =
                latexRecordRepository.findByTimestampBetween(startOfDay, endOfDay)
                        .stream().map(LatexRecordMapper::toResponse).collect(Collectors.toList());

        List<com.madukotawatte.erp.dto.expense.ExpenseResponse> expenses =
                expenseRepository.findByTimestampBetween(startOfDay, endOfDay, PageRequest.of(0, Integer.MAX_VALUE))
                        .stream().map(ExpenseMapper::toResponse).collect(Collectors.toList());

        // Rubber solid records via loads in the date range
        List<com.madukotawatte.erp.dto.rubbersolid.RubberSolidRecordResponse> rubberSolidRecords =
                loadRepository.findByStartDateBetween(startOfDay, endOfDay, PageRequest.of(0, Integer.MAX_VALUE))
                        .stream()
                        .flatMap(load -> rubberSolidRecordRepository.findByLoad_LoadId(load.getLoadId()).stream())
                        .map(RubberSolidMapper::toResponse)
                        .collect(Collectors.toList());

        return DailyReportResponse.builder()
                .date(date)
                .attendances(attendances)
                .latexRecords(latexRecords)
                .expenses(expenses)
                .rubberSolidRecords(rubberSolidRecords)
                .build();
    }

    public MonthlyPayrollResponse getMonthlyPayroll(int month, int year) {
        LocalDateTime startOfMonth = LocalDate.of(year, month, 1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        List<Employee> employees = employeeRepository.findAll();
        List<EmployeePayrollEntry> entries = new ArrayList<>();

        for (Employee employee : employees) {
            String employeeId = employee.getEmployeeId();

            // Count work days (attendances with noWork == "none")
            List<Attendance> attendances = attendanceRepository
                    .findByEmployee_EmployeeIdAndTimestampBetween(employeeId, startOfMonth, endOfMonth);
            int workDays = (int) attendances.stream()
                    .filter(a -> "none".equals(a.getNoWork()))
                    .count();

            // Sum transactions by type
            BigDecimal latexTap = sumTransactionsByType(employeeId, "Latex_Tap", startOfMonth, endOfMonth);
            BigDecimal advances = sumTransactionsByType(employeeId, "Advance", startOfMonth, endOfMonth);
            BigDecimal loanDeductions = sumTransactionsByType(employeeId, "Loan_Payment", startOfMonth, endOfMonth);
            BigDecimal manualLabor = sumTransactionsByType(employeeId, "Manual_Labor", startOfMonth, endOfMonth);

            // net = salary + latexTap + manualLabor - advances - loanDeductions
            BigDecimal salary = employee.getSalary() != null ? employee.getSalary() : BigDecimal.ZERO;
            BigDecimal net = salary.add(latexTap).add(manualLabor).subtract(advances).subtract(loanDeductions);

            entries.add(EmployeePayrollEntry.builder()
                    .employeeId(employeeId)
                    .employeeName(employee.getName())
                    .salary(salary)
                    .workDays(workDays)
                    .latexTap(latexTap)
                    .advances(advances)
                    .loanDeductions(loanDeductions)
                    .manualLabor(manualLabor)
                    .net(net)
                    .build());
        }

        return MonthlyPayrollResponse.builder()
                .month(month)
                .year(year)
                .entries(entries)
                .build();
    }

    public byte[] exportPayroll(int month, int year) {
        MonthlyPayrollResponse payroll = getMonthlyPayroll(month, year);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Payroll " + month + "-" + year);

            // Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Employee ID", "Name", "Salary", "Work Days",
                    "Latex Tap", "Manual Labor", "Advances", "Loan Deductions", "Net"};
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
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
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate payroll export", e);
        }
    }

    private BigDecimal sumTransactionsByType(String employeeId, String type,
                                              LocalDateTime from, LocalDateTime to) {
        return employeeTransactionRepository
                .findByEmployee_EmployeeIdAndTimestampBetween(employeeId, from, to,
                        PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(t -> type.equals(t.getType()))
                .map(EmployeeTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
