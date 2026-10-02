package com.madukotawatte.erp.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Pure payroll arithmetic (no database), kept separate so it can be unit tested. */
public final class PayrollCalculator {

    private PayrollCalculator() {}

    /** An inclusive date range already covered by a payslip's fixed-salary component. */
    public record DateRange(LocalDate start, LocalDate end) {
        boolean contains(LocalDate d) {
            return !d.isBefore(start) && !d.isAfter(end);
        }
    }

    /** Suggested deductions for a payslip. */
    public record Deductions(BigDecimal advance, BigDecimal loan, BigDecimal net) {}

    /**
     * Fixed monthly salary earned in [from, to]: every calendar month contributes salary x (uncovered days in
     * range / days in month), so a whole month yields exactly the monthly salary and a partial month is pro-rated
     * by days. Days before {@code joinedDate} and days already covered by an earlier payslip are not paid again.
     */
    public static BigDecimal fixedSalary(BigDecimal salary, LocalDate from, LocalDate to, LocalDate joinedDate,
                                         List<DateRange> covered) {
        if (salary == null || salary.signum() <= 0 || to.isBefore(from)) return money(BigDecimal.ZERO);
        LocalDate start = joinedDate != null && joinedDate.isAfter(from) ? joinedDate : from;
        BigDecimal total = BigDecimal.ZERO;
        for (YearMonth ym = YearMonth.from(start); !ym.isAfter(YearMonth.from(to)); ym = ym.plusMonths(1)) {
            LocalDate segStart = ym.atDay(1).isBefore(start) ? start : ym.atDay(1);
            LocalDate segEnd = ym.atEndOfMonth().isAfter(to) ? to : ym.atEndOfMonth();
            int days = 0;
            for (LocalDate d = segStart; !d.isAfter(segEnd); d = d.plusDays(1)) {
                final LocalDate day = d;
                if (covered == null || covered.stream().noneMatch(r -> r.contains(day))) days++;
            }
            if (days > 0) {
                total = total.add(salary.multiply(BigDecimal.valueOf(days))
                        .divide(BigDecimal.valueOf(ym.lengthOfMonth()), 6, RoundingMode.HALF_UP));
            }
        }
        return money(total);
    }

    /**
     * Default deductions: advances first (up to what is outstanding and what the gross can bear), then the loan
     * installment (up to the balance and what remains of the gross).
     */
    public static Deductions suggestDeductions(BigDecimal gross, BigDecimal outstandingAdvances,
                                               BigDecimal loanBalance, BigDecimal installment) {
        BigDecimal advance = outstandingAdvances.max(BigDecimal.ZERO).min(gross).max(BigDecimal.ZERO);
        BigDecimal loan = BigDecimal.ZERO;
        if (loanBalance != null && loanBalance.signum() > 0 && installment != null) {
            loan = installment.min(loanBalance).min(gross.subtract(advance)).max(BigDecimal.ZERO);
        }
        return new Deductions(money(advance), money(loan), net(gross, advance, loan, BigDecimal.ZERO));
    }

    /** gross - advance recovery - loan deduction - other deduction. */
    public static BigDecimal net(BigDecimal gross, BigDecimal advance, BigDecimal loan, BigDecimal other) {
        return money(gross.subtract(advance).subtract(loan).subtract(other));
    }

    public static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
