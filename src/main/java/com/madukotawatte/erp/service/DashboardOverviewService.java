package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.account.AccountBalanceResponse;
import com.madukotawatte.erp.dto.dashboard.DashboardOverviewResponse;
import com.madukotawatte.erp.dto.dashboard.DashboardOverviewResponse.*;
import com.madukotawatte.erp.dto.employee.PaymentSummaryResponse;
import com.madukotawatte.erp.dto.estateloan.LoanBalanceAsOfResponse;
import com.madukotawatte.erp.dto.finance.PayablesResponse;
import com.madukotawatte.erp.dto.finance.PayableRowResponse;
import com.madukotawatte.erp.dto.finance.ReceivableRowResponse;
import com.madukotawatte.erp.dto.finance.ReceivablesResponse;
import com.madukotawatte.erp.dto.payroll.PayrollSummaryRow;
import com.madukotawatte.erp.dto.weather.WeatherOutlookResponse;
import com.madukotawatte.erp.entity.AccountType;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.repository.SalesLedgerRepository;
import com.madukotawatte.erp.service.DashboardPeriods.Range;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds the dashboard overview from the real operational tables and the Wave 1-2 services. */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardOverviewService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CARD_ALERT_PCT = new BigDecimal("80");
    private static final int TREND_MONTHS = 12;

    private final NamedParameterJdbcTemplate jdbc;
    private final SalesLedgerRepository salesLedgerRepository;
    private final LedgerService ledgerService;
    private final ReceivablesService receivablesService;
    private final PayablesService payablesService;
    private final EstateLoanService estateLoanService;
    private final DepreciationService depreciationService;
    private final PayrollService payrollService;
    private final WeatherOutlookService weatherOutlookService;

    @Value("${app.estate.timezone:Asia/Colombo}")
    private String timezone;

    @Value("${app.dashboard.low-balance-threshold:10000}")
    private BigDecimal lowBalanceThreshold;

    public DashboardOverviewResponse overview(String monthText) {
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        YearMonth month;
        try {
            month = monthText == null || monthText.isBlank() ? YearMonth.from(today) : YearMonth.parse(monthText);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("month must be in the format YYYY-MM");
        }
        LocalDate asOf = DashboardPeriods.asOf(month, today);

        Money money = buildMoney(today);
        Range mtd = DashboardPeriods.monthToDate(month, today);
        Periods periods = new Periods(
                figures("Month to date", mtd),
                figures("Previous month (same days)", DashboardPeriods.previousMonthSameDays(month, today)),
                figures("Previous month", DashboardPeriods.previousMonthFull(month)),
                figures("Calendar year to date", DashboardPeriods.calendarYearToDate(asOf)),
                figures("Financial year " + DashboardPeriods.financialYearLabel(asOf) + " to date",
                        DashboardPeriods.financialYearToDate(asOf)));

        Production production = buildProduction(month, today, mtd);
        Workforce workforce = buildWorkforce(month, today);
        return new DashboardOverviewResponse(today, month.toString(), money, periods, buildTrend(month),
                production, workforce, buildAlerts(money, today));
    }

    // ── money ───────────────────────────────────────────────────────────────

    private Money buildMoney(LocalDate today) {
        List<AccountLine> cash = new ArrayList<>();
        List<AccountLine> cards = new ArrayList<>();
        BigDecimal cashTotal = ZERO;
        BigDecimal owed = ZERO;
        BigDecimal available = ZERO;
        for (AccountBalanceResponse b : ledgerService.getBalances(null)) {
            if (!Boolean.TRUE.equals(b.getIsActive())) continue;
            if (b.getType() == AccountType.CREDIT_CARD) {
                BigDecimal cardOwed = b.getBalance().negate().max(ZERO);
                BigDecimal util = b.getCreditLimit() != null && b.getCreditLimit().signum() > 0
                        ? cardOwed.multiply(BigDecimal.valueOf(100)).divide(b.getCreditLimit(), 1, RoundingMode.HALF_UP) : null;
                cards.add(new AccountLine(b.getAccountCode(), b.getName(), b.getType().name(), b.getBalance(),
                        b.getCreditLimit(), b.getAvailableCredit(), util));
                owed = owed.add(cardOwed);
                if (b.getAvailableCredit() != null) available = available.add(b.getAvailableCredit());
            } else {
                cash.add(new AccountLine(b.getAccountCode(), b.getName(), b.getType().name(), b.getBalance(),
                        null, null, null));
                cashTotal = cashTotal.add(b.getBalance());
            }
        }

        ReceivablesResponse rec = receivablesService.getReceivables(today);
        Party receivables = new Party(rec.getTotal(), rec.getOverdueTotal(), rec.getRows().size(),
                (int) rec.getRows().stream().filter(ReceivableRowResponse::isOverdue).count());
        PayablesResponse pay = payablesService.getPayables(today);
        Party payables = new Party(pay.getTotal(), pay.getOverdueTotal(), pay.getRows().size(),
                (int) pay.getRows().stream().filter(PayableRowResponse::isOverdue).count());

        List<NamedAmount> loans = new ArrayList<>();
        BigDecimal borrowings = ZERO;
        for (LoanBalanceAsOfResponse l : estateLoanService.balancesAsOf(today)) {
            if (l.getBalance().signum() > 0) {
                loans.add(new NamedAmount(l.getName(), l.getBalance()));
                borrowings = borrowings.add(l.getBalance());
            }
        }

        PaymentSummaryResponse ps = payrollService.paymentSummary();
        Integer activeLoans = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM employee_loans WHERE is_active = TRUE", Integer.class);

        return new Money(cashTotal, cash, owed, available, cards, receivables, payables, borrowings, loans,
                ps.getOutstandingAdvances(), ps.getActiveEmployeeLoansTotal(), activeLoans == null ? 0 : activeLoans);
    }

    // ── periods ─────────────────────────────────────────────────────────────

    private PeriodFigures figures(String label, Range range) {
        List<NamedAmount> revenueByCrop = new ArrayList<>();
        BigDecimal revenue = ZERO;
        for (Object[] row : salesLedgerRepository.sumByCategory(range.from(), range.to())) {
            BigDecimal amount = toBigDecimal(row[1]);
            if (amount.signum() == 0) continue;
            revenueByCrop.add(new NamedAmount(cropLabel((String) row[0]), amount));
            revenue = revenue.add(amount);
        }
        revenueByCrop.sort(Comparator.comparing(NamedAmount::amount).reversed());

        List<NamedAmount> byCategory = jdbc.query(
                "SELECT type, COALESCE(SUM(amount), 0) AS total FROM expenses "
                        + "WHERE timestamp >= :from AND timestamp < :to GROUP BY type ORDER BY total DESC",
                rangeParams(range),
                (rs, i) -> new NamedAmount(rs.getString("type"), rs.getBigDecimal("total")));
        BigDecimal expenses = byCategory.stream().map(NamedAmount::amount).reduce(ZERO, BigDecimal::add);

        BigDecimal depreciation = depreciationService.schedule(range.from(), range.to()).getTotal().getChargeForPeriod();
        if (depreciation == null) depreciation = ZERO;
        BigDecimal operating = revenue.subtract(expenses);
        return new PeriodFigures(label, range.from(), range.to(), revenue, expenses, operating, depreciation,
                operating.subtract(depreciation), revenueByCrop, byCategory);
    }

    private List<TrendPoint> buildTrend(YearMonth month) {
        YearMonth first = month.minusMonths(TREND_MONTHS - 1L);
        LocalDate from = first.atDay(1);
        LocalDate to = month.atEndOfMonth();
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        Map<String, BigDecimal> expenses = new LinkedHashMap<>();
        Map<String, BigDecimal> latex = new LinkedHashMap<>();
        for (Object[] row : salesLedgerRepository.findAmountsInRange(from, to)) {
            LocalDate d = row[0] instanceof Date sd ? sd.toLocalDate() : LocalDate.parse(row[0].toString());
            revenue.merge(YearMonth.from(d).toString(), toBigDecimal(row[1]), BigDecimal::add);
        }
        MapSqlParameterSource p = rangeParams(new Range(from, to));
        jdbc.query("SELECT to_char(timestamp, 'YYYY-MM') AS m, SUM(amount) AS total FROM expenses "
                        + "WHERE timestamp >= :from AND timestamp < :to GROUP BY 1", p,
                rs -> { expenses.put(rs.getString("m"), rs.getBigDecimal("total")); });
        jdbc.query("SELECT to_char(timestamp, 'YYYY-MM') AS m, SUM(latex_amount) AS total FROM latex_records "
                        + "WHERE timestamp >= :from AND timestamp < :to GROUP BY 1", p,
                rs -> { latex.put(rs.getString("m"), rs.getBigDecimal("total")); });
        List<TrendPoint> points = new ArrayList<>();
        for (YearMonth m = first; !m.isAfter(month); m = m.plusMonths(1)) {
            String key = m.toString();
            points.add(new TrendPoint(key, revenue.getOrDefault(key, ZERO), expenses.getOrDefault(key, ZERO),
                    latex.getOrDefault(key, ZERO)));
        }
        return points;
    }

    // ── production ──────────────────────────────────────────────────────────

    private Production buildProduction(YearMonth month, LocalDate today, Range mtd) {
        BigDecimal latexToday = sumBetween("latex_records", "latex_amount", new Range(today, today));
        BigDecimal latexMtd = sumBetween("latex_records", "latex_amount", mtd);
        BigDecimal latexPrev = sumBetween("latex_records", "latex_amount",
                DashboardPeriods.previousMonthSameDays(month, today));
        BigDecimal rubber = sumBetween("rubber_solid_records", "mass_kg", mtd);
        BigDecimal trees = jdbc.queryForObject(
                "SELECT COALESCE(SUM(no_of_trees), 0) FROM attendance WHERE no_work = 'none' "
                        + "AND timestamp >= :from AND timestamp < :to", rangeParams(mtd), BigDecimal.class);
        trees = trees == null ? ZERO : trees;
        BigDecimal perTree = trees.signum() > 0 ? latexMtd.divide(trees, 4, RoundingMode.HALF_UP) : null;

        List<CropQuantity> crops = new ArrayList<>();
        addCrop(crops, "Coconut", "nuts", "coconut_records", "nut_count", mtd);
        addCrop(crops, "Banana", "bunches", "banana_records", "bunch_count", mtd);
        addCrop(crops, "Manioc", "kg", "manioc_records", "mass_kg", mtd);
        crops.addAll(jdbc.query(
                "SELECT crop, unit, SUM(quantity) AS qty, COUNT(*) AS n FROM crop_harvests "
                        + "WHERE harvest_date >= :from AND harvest_date <= :to GROUP BY crop, unit ORDER BY crop",
                new MapSqlParameterSource("from", Date.valueOf(mtd.from())).addValue("to", Date.valueOf(mtd.to())),
                (rs, i) -> new CropQuantity(rs.getString("crop"), rs.getString("unit"), rs.getBigDecimal("qty"),
                        rs.getLong("n"))));
        return new Production(latexToday, latexMtd, latexPrev, rubber, trees, perTree, crops);
    }

    private void addCrop(List<CropQuantity> out, String crop, String unit, String table, String column, Range range) {
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT COALESCE(SUM(" + column + "), 0) AS qty, COUNT(*) AS n FROM " + table
                        + " WHERE timestamp >= :from AND timestamp < :to", rangeParams(range));
        long n = ((Number) row.get("n")).longValue();
        if (n > 0) out.add(new CropQuantity(crop, unit, toBigDecimal(row.get("qty")), n));
    }

    private BigDecimal sumBetween(String table, String column, Range range) {
        BigDecimal v = jdbc.queryForObject("SELECT COALESCE(SUM(" + column + "), 0) FROM " + table
                + " WHERE timestamp >= :from AND timestamp < :to", rangeParams(range), BigDecimal.class);
        return v == null ? ZERO : v;
    }

    // ── workforce ───────────────────────────────────────────────────────────

    private Workforce buildWorkforce(YearMonth month, LocalDate today) {
        List<NamedCount> byPayType = jdbc.query(
                "SELECT pay_type, COUNT(*) AS n FROM employees WHERE is_active = TRUE GROUP BY pay_type ORDER BY pay_type",
                new MapSqlParameterSource(), (rs, i) -> new NamedCount(rs.getString("pay_type"), rs.getInt("n")));
        int active = byPayType.stream().mapToInt(NamedCount::count).sum();

        Map<String, Integer> byReason = new LinkedHashMap<>();
        jdbc.query("SELECT COALESCE(a.no_work, 'none') AS reason, COUNT(DISTINCT a.employee_id) AS n "
                        + "FROM attendance a JOIN employees e ON e.employee_id = a.employee_id AND e.is_active = TRUE "
                        + "WHERE a.timestamp >= :from AND a.timestamp < :to GROUP BY 1 ORDER BY 1",
                rangeParams(new Range(today, today)), rs -> { byReason.put(rs.getString("reason"), rs.getInt("n")); });
        int present = byReason.getOrDefault("none", 0);
        List<NamedCount> noWork = byReason.entrySet().stream().filter(e -> !e.getKey().equals("none"))
                .map(e -> new NamedCount(e.getKey(), e.getValue())).toList();
        int logged = byReason.values().stream().mapToInt(Integer::intValue).sum();

        PayrollSummaryRow paid = payrollService.summary(month.atDay(1), month.atEndOfMonth()).getTotals();
        PaymentSummaryResponse ps = payrollService.paymentSummary();
        return new Workforce(active, byPayType, present, noWork, Math.max(active - logged, 0),
                paid.getPayslipCount(), paid.getGross(), paid.getNet(), ps.getUnpaidEarnings());
    }

    // ── alerts ──────────────────────────────────────────────────────────────

    private List<Alert> buildAlerts(Money money, LocalDate today) {
        List<Alert> alerts = new ArrayList<>();
        if (money.receivables().overdueCount() > 0) {
            alerts.add(new Alert("OVERDUE_RECEIVABLES", "WARNING", "Overdue customer payments",
                    money.receivables().overdueCount() + " sale(s) past their due date",
                    money.receivables().overdue(), "/financials"));
        }
        if (money.payables().overdueCount() > 0) {
            alerts.add(new Alert("OVERDUE_PAYABLES", "DANGER", "Overdue bills to pay",
                    money.payables().overdueCount() + " expense(s) past their due date",
                    money.payables().overdue(), "/financials"));
        }
        for (AccountLine card : money.creditCards()) {
            if (card.utilisationPct() != null && card.utilisationPct().compareTo(CARD_ALERT_PCT) >= 0) {
                alerts.add(new Alert("CARD_UTILISATION", "WARNING",
                        card.name() + " is " + card.utilisationPct().stripTrailingZeros().toPlainString() + "% used",
                        "Available credit " + card.availableCredit().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        card.balance().negate(), "/assets"));
            }
        }
        for (AccountLine acc : money.cashAndBank()) {
            if (acc.balance().compareTo(lowBalanceThreshold) < 0) {
                alerts.add(new Alert("LOW_BALANCE", "WARNING", acc.name() + " balance is low",
                        "Below the " + lowBalanceThreshold.setScale(0, RoundingMode.HALF_UP).toPlainString() + " threshold",
                        acc.balance(), "/assets"));
            }
        }
        if (money.activeEmployeeLoans() > 0) {
            alerts.add(new Alert("EMPLOYEE_LOANS", "INFO", "Active employee loans",
                    money.activeEmployeeLoans() + " loan(s) being recovered from pay",
                    money.employeeLoansOutstanding(), "/employees"));
        }
        try {
            LocalDate tomorrow = today.plusDays(1);
            for (WeatherOutlookResponse.Day d : weatherOutlookService.outlook().days()) {
                if (d.date().equals(tomorrow) && !d.tappable()) {
                    alerts.add(new Alert("TOMORROW_NOT_TAPPABLE", "INFO", "Tomorrow is not tappable",
                            d.summary(), null, "/weather"));
                }
            }
        } catch (RuntimeException e) {
            log.warn("Weather outlook unavailable for the dashboard: {}", e.getMessage());
        }
        return alerts;
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static MapSqlParameterSource rangeParams(Range range) {
        return new MapSqlParameterSource("from", range.from().atStartOfDay())
                .addValue("to", range.to().plusDays(1).atStartOfDay());
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) return ZERO;
        return value instanceof BigDecimal bd ? bd : new BigDecimal(value.toString());
    }

    private static String cropLabel(String category) {
        return switch (category) {
            case "latex" -> "Latex";
            case "rubber-solid" -> "Rubber (sheets/solid)";
            case "manioc" -> "Manioc";
            case "coconut" -> "Coconut";
            case "banana" -> "Banana";
            default -> category;
        };
    }
}
