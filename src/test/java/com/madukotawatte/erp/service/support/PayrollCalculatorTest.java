package com.madukotawatte.erp.service.support;

import com.madukotawatte.erp.service.support.PayrollCalculator.DateRange;
import com.madukotawatte.erp.service.support.PayrollCalculator.Deductions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayrollCalculatorTest {

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("a whole calendar month pays the full monthly salary")
    void wholeMonth() {
        assertEquals(bd("45000.00"), PayrollCalculator.fixedSalary(bd("45000"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, List.of()));
    }

    @Test
    @DisplayName("two whole months pay two salaries")
    void twoMonths() {
        assertEquals(bd("90000.00"), PayrollCalculator.fixedSalary(bd("45000"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30), null, List.of()));
    }

    @Test
    @DisplayName("a partial month is pro-rated by days (10 of 30)")
    void partialMonth() {
        assertEquals(bd("15000.00"), PayrollCalculator.fixedSalary(bd("45000"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10), null, List.of()));
    }

    @Test
    @DisplayName("days already covered by an earlier payslip are not paid again")
    void coveredDaysExcluded() {
        List<DateRange> covered = List.of(new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)));
        assertEquals(bd("22500.00"), PayrollCalculator.fixedSalary(bd("45000"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, covered));
        List<DateRange> whole = List.of(new DateRange(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
        assertEquals(bd("0.00"), PayrollCalculator.fixedSalary(bd("45000"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, whole));
    }

    @Test
    @DisplayName("days before the joined date are not paid")
    void joinedMidMonth() {
        assertEquals(bd("15000.00"), PayrollCalculator.fixedSalary(bd("45000"),
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 21), List.of()));
    }

    @Test
    @DisplayName("no salary, no fixed pay")
    void noSalary() {
        assertEquals(bd("0.00"), PayrollCalculator.fixedSalary(null,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null, List.of()));
    }

    @Test
    @DisplayName("suggested deductions never exceed gross, advances first then loan installment")
    void suggestDeductions() {
        Deductions d = PayrollCalculator.suggestDeductions(bd("50000"), bd("10000"), bd("30000"), bd("5000"));
        assertEquals(bd("10000.00"), d.advance());
        assertEquals(bd("5000.00"), d.loan());
        assertEquals(bd("35000.00"), d.net());
    }

    @Test
    @DisplayName("advance capped by gross; loan capped by balance and what is left")
    void suggestCaps() {
        Deductions small = PayrollCalculator.suggestDeductions(bd("8000"), bd("10000"), bd("30000"), bd("5000"));
        assertEquals(bd("8000.00"), small.advance());
        assertEquals(bd("0.00"), small.loan());
        assertEquals(bd("0.00"), small.net());

        Deductions balance = PayrollCalculator.suggestDeductions(bd("50000"), bd("0"), bd("2000"), bd("5000"));
        assertEquals(bd("2000.00"), balance.loan());
        assertEquals(bd("48000.00"), balance.net());

        Deductions leftover = PayrollCalculator.suggestDeductions(bd("12000"), bd("10000"), bd("30000"), bd("5000"));
        assertEquals(bd("2000.00"), leftover.loan());
    }

    @Test
    @DisplayName("gross = net + advance + loan + other deduction")
    void netIdentity() {
        BigDecimal gross = bd("60000");
        BigDecimal net = PayrollCalculator.net(gross, bd("10000"), bd("5000"), bd("1500.50"));
        assertEquals(bd("43499.50"), net);
        assertEquals(PayrollCalculator.money(gross),
                net.add(bd("10000")).add(bd("5000")).add(bd("1500.50")));
    }
}
