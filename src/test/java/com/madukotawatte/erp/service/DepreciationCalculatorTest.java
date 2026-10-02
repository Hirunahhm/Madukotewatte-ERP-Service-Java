package com.madukotawatte.erp.service;

import com.madukotawatte.erp.entity.FixedAsset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DepreciationCalculatorTest {

    private static FixedAsset vehicle() {
        // 6,000,000 cost, 600,000 residual, 5 years -> 90,000 per month; bought 15 Mar 2024
        return FixedAsset.builder()
                .acquisitionDate(LocalDate.of(2024, 3, 15))
                .acquisitionValue(new BigDecimal("6000000.00"))
                .residualValue(new BigDecimal("600000.00"))
                .usefulLifeYears(5)
                .depreciationMethod("STRAIGHT_LINE")
                .build();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    @DisplayName("acquisition month counts in full once its month-end is reached; mid-month dates exclude the current month")
    void partialMonths() {
        FixedAsset a = vehicle();
        assertEquals(0, DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2024, 3, 14)).signum());
        assertEquals(0, DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2024, 3, 30)).signum());
        assertEquals(0, bd("90000.00").compareTo(DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2024, 3, 31))));
        assertEquals(0, bd("90000.00").compareTo(DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2024, 4, 20))));
        // Mar..Jun = 4 months
        assertEquals(0, bd("360000.00").compareTo(DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2024, 6, 30))));
    }

    @Test
    @DisplayName("accumulated depreciation never goes below residual value")
    void residualFloor() {
        FixedAsset a = vehicle();
        // 60 months: Mar 2024 .. Feb 2029
        assertEquals(0, bd("5400000.00").compareTo(DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2029, 2, 28))));
        assertEquals(0, bd("5400000.00").compareTo(DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2035, 12, 31))));
        assertEquals(0, bd("600000.00").compareTo(DepreciationCalculator.nbvAt(a, LocalDate.of(2035, 12, 31))));
    }

    @Test
    @DisplayName("disposal stops depreciation after the disposal month and removes the asset from the books")
    void disposal() {
        FixedAsset a = vehicle();
        a.setDisposalDate(LocalDate.of(2024, 9, 10));
        assertTrue(DepreciationCalculator.heldAt(a, LocalDate.of(2024, 9, 9)));
        assertFalse(DepreciationCalculator.heldAt(a, LocalDate.of(2024, 9, 10)));
        // Mar..Sep = 7 months charged through the disposal month
        assertEquals(0, bd("630000.00").compareTo(DepreciationCalculator.accumulatedThrough(a, LocalDate.of(2024, 12, 31))));
        assertEquals(0, DepreciationCalculator.nbvAt(a, LocalDate.of(2024, 12, 31)).signum());
        // NBV at disposal = 6,000,000 - 630,000; proceeds 5,000,000 gives a loss of 370,000
        BigDecimal nbv = DepreciationCalculator.nbvAtDisposal(vehicle(), LocalDate.of(2024, 9, 10));
        assertEquals(0, bd("5370000.00").compareTo(nbv));
        assertEquals(0, bd("-370000.00").compareTo(bd("5000000").subtract(nbv)));
    }

    @Test
    @DisplayName("method NONE (land) is never depreciated; defaults per category")
    void noneAndDefaults() {
        FixedAsset land = FixedAsset.builder()
                .acquisitionDate(LocalDate.of(2010, 1, 1))
                .acquisitionValue(new BigDecimal("15000000.00"))
                .residualValue(BigDecimal.ZERO)
                .depreciationMethod("NONE")
                .build();
        assertEquals(0, DepreciationCalculator.accumulatedThrough(land, LocalDate.of(2026, 1, 1)).signum());
        assertNull(DepreciationCalculator.defaultLifeYears("Land"));
        assertEquals("NONE", DepreciationCalculator.defaultMethod("land"));
        assertEquals(20, DepreciationCalculator.defaultLifeYears("Building"));
        assertEquals(5, DepreciationCalculator.defaultLifeYears("Vehicle"));
        assertEquals(10, DepreciationCalculator.defaultLifeYears("Machinery"));
        assertEquals(25, DepreciationCalculator.defaultLifeYears("Plantation"));
    }

    @Test
    @DisplayName("quarter charge = accumulated(end) - accumulated(day before start)")
    void periodCharge() {
        FixedAsset a = vehicle();
        BigDecimal start = DepreciationCalculator.accumulatedAt(a, LocalDate.of(2025, 3, 31));
        BigDecimal end = DepreciationCalculator.accumulatedAt(a, LocalDate.of(2025, 6, 30));
        assertEquals(0, bd("270000.00").compareTo(end.subtract(start)));
    }
}
