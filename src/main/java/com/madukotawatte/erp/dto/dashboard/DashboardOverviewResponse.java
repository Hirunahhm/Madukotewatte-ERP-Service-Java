package com.madukotawatte.erp.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** One-call payload of the dashboard overview (money, periods, trend, production, workforce, alerts). */
public record DashboardOverviewResponse(
        LocalDate asOf,
        String month,
        Money money,
        Periods periods,
        List<TrendPoint> trend,
        Production production,
        Workforce workforce,
        List<Alert> alerts
) {

    public record NamedAmount(String name, BigDecimal amount) {
    }

    // ── money ───────────────────────────────────────────────────────────────

    public record AccountLine(String code, String name, String type, BigDecimal balance, BigDecimal creditLimit,
                              BigDecimal availableCredit, BigDecimal utilisationPct) {
    }

    public record Party(BigDecimal total, BigDecimal overdue, int count, int overdueCount) {
    }

    public record Money(
            BigDecimal cashAndBankTotal,
            List<AccountLine> cashAndBank,
            BigDecimal creditCardsOwed,
            BigDecimal creditCardsAvailable,
            List<AccountLine> creditCards,
            Party receivables,
            Party payables,
            BigDecimal borrowingsOutstanding,
            List<NamedAmount> borrowings,
            BigDecimal employeeAdvancesOutstanding,
            BigDecimal employeeLoansOutstanding,
            int activeEmployeeLoans
    ) {
    }

    // ── periods ─────────────────────────────────────────────────────────────

    public record PeriodFigures(
            String label,
            LocalDate from,
            LocalDate to,
            BigDecimal revenue,
            BigDecimal expenses,
            BigDecimal operatingResult,
            BigDecimal depreciation,
            BigDecimal resultAfterDepreciation,
            List<NamedAmount> revenueByCrop,
            List<NamedAmount> expensesByCategory
    ) {
    }

    public record Periods(
            PeriodFigures monthToDate,
            PeriodFigures previousMonthSameDays,
            PeriodFigures previousMonth,
            PeriodFigures yearToDate,
            PeriodFigures financialYearToDate
    ) {
    }

    public record TrendPoint(String month, BigDecimal revenue, BigDecimal expenses, BigDecimal latexLitres) {
    }

    // ── production ──────────────────────────────────────────────────────────

    public record CropQuantity(String crop, String unit, BigDecimal quantity, long harvests) {
    }

    public record Production(
            BigDecimal latexToday,
            BigDecimal latexMonthToDate,
            BigDecimal latexPreviousMonthSameDays,
            BigDecimal rubberKgMonthToDate,
            BigDecimal treesTappedMonthToDate,
            BigDecimal latexPerTree,
            List<CropQuantity> crops
    ) {
    }

    // ── workforce ───────────────────────────────────────────────────────────

    public record Workforce(
            int activeEmployees,
            List<NamedCount> byPayType,
            int presentToday,
            List<NamedCount> noWorkToday,
            int notLoggedToday,
            int payslipsPaidInMonth,
            BigDecimal payrollGrossPaidInMonth,
            BigDecimal payrollNetPaidInMonth,
            BigDecimal unpaidEarningsToDate
    ) {
    }

    public record NamedCount(String name, int count) {
    }

    // ── alerts ──────────────────────────────────────────────────────────────

    /** severity: DANGER | WARNING | INFO. href is a frontend route. */
    public record Alert(String type, String severity, String title, String detail, BigDecimal amount, String href) {
    }
}
