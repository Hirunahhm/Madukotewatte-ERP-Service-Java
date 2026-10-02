package com.madukotawatte.erp.service;

import com.madukotawatte.erp.entity.FixedAsset;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Pure straight-line depreciation maths (no Spring, no DB).
 *
 * <p>Convention: depreciation is booked monthly. The acquisition month is charged in full; a month is charged
 * once its last day has been reached (so a mid-month "as of" date excludes the current month); in the month of
 * disposal the full month is charged. Accumulated depreciation never exceeds (cost - residual).
 */
public final class DepreciationCalculator {

    public static final String STRAIGHT_LINE = "STRAIGHT_LINE";
    public static final String NONE = "NONE";

    private DepreciationCalculator() {}

    /** Whether the asset is on the books (acquired and not yet disposed) on the given date. */
    public static boolean heldAt(FixedAsset a, LocalDate date) {
        if (a.getAcquisitionDate().isAfter(date)) return false;
        return a.getDisposalDate() == null || a.getDisposalDate().isAfter(date);
    }

    /** Number of months charged up to and including {@code date} (see class convention). */
    public static long monthsCharged(FixedAsset a, LocalDate date) {
        YearMonth first = YearMonth.from(a.getAcquisitionDate());
        YearMonth last;
        if (a.getDisposalDate() != null && !date.isBefore(a.getDisposalDate())) {
            last = YearMonth.from(a.getDisposalDate());
        } else if (date.equals(date.withDayOfMonth(date.lengthOfMonth()))) {
            last = YearMonth.from(date);
        } else {
            last = YearMonth.from(date).minusMonths(1);
        }
        if (last.isBefore(first)) return 0;
        return ChronoUnit.MONTHS.between(first, last) + 1;
    }

    /**
     * Accumulated depreciation as at {@code date}, ignoring whether the asset has since left the books
     * (after disposal the figure freezes at the disposal-month value). Zero before acquisition.
     */
    public static BigDecimal accumulatedThrough(FixedAsset a, LocalDate date) {
        if (a.getAcquisitionDate().isAfter(date)) return zero();
        if (!STRAIGHT_LINE.equals(a.getDepreciationMethod())
                || a.getUsefulLifeYears() == null || a.getUsefulLifeYears() <= 0) {
            return zero();
        }
        BigDecimal residual = a.getResidualValue() != null ? a.getResidualValue() : BigDecimal.ZERO;
        BigDecimal depreciable = a.getAcquisitionValue().subtract(residual).max(BigDecimal.ZERO);
        long totalMonths = a.getUsefulLifeYears() * 12L;
        long n = Math.min(monthsCharged(a, date), totalMonths);
        if (n <= 0) return zero();
        return depreciable.multiply(BigDecimal.valueOf(n))
                .divide(BigDecimal.valueOf(totalMonths), 2, RoundingMode.HALF_UP)
                .min(depreciable);
    }

    /** Accumulated depreciation on the books at {@code date} (zero when not held). */
    public static BigDecimal accumulatedAt(FixedAsset a, LocalDate date) {
        return heldAt(a, date) ? accumulatedThrough(a, date) : zero();
    }

    /** Net book value on the books at {@code date} (zero when not held). */
    public static BigDecimal nbvAt(FixedAsset a, LocalDate date) {
        return heldAt(a, date) ? a.getAcquisitionValue().subtract(accumulatedThrough(a, date)) : zero();
    }

    /** NBV at the moment of disposal (cost less depreciation through the disposal month). */
    public static BigDecimal nbvAtDisposal(FixedAsset a, LocalDate disposalDate) {
        FixedAsset probe = FixedAsset.builder()
                .acquisitionDate(a.getAcquisitionDate())
                .acquisitionValue(a.getAcquisitionValue())
                .residualValue(a.getResidualValue())
                .usefulLifeYears(a.getUsefulLifeYears())
                .depreciationMethod(a.getDepreciationMethod())
                .disposalDate(disposalDate)
                .build();
        return a.getAcquisitionValue().subtract(accumulatedThrough(probe, disposalDate));
    }

    /** Default useful life by category (null = not depreciated). Same choices as the V21 migration. */
    public static Integer defaultLifeYears(String category) {
        String c = category == null ? "" : category.trim().toLowerCase();
        return switch (c) {
            case "land" -> null;
            case "building" -> 20;
            case "vehicle" -> 5;
            case "machinery" -> 10;
            case "equipment", "tools" -> 5;
            case "plantation", "mature rubber", "trees" -> 25;
            default -> 5;
        };
    }

    public static String defaultMethod(String category) {
        return defaultLifeYears(category) == null ? NONE : STRAIGHT_LINE;
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
