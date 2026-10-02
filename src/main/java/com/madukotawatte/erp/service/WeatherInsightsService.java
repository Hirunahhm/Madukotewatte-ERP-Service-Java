package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse;
import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse.Correlations;
import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse.Day;
import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse.Month;
import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse.WetDryYield;
import com.madukotawatte.erp.dto.weather.WeatherInsightsResponse.YearToDate;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.repository.WeatherDailyRepository;
import com.madukotawatte.erp.repository.WeatherDailyRepository.WeatherRow;
import com.madukotawatte.erp.repository.WeatherEstateActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Joins weather_daily with estate activity.
 * <p>
 * Yield basis: {@code latex_records.latex_amount} summed per record date (litres of field latex as recorded).
 * Dry rubber weight is not derivable per day (DRC comes from load-level metrolac readings), so litres are used.
 * A "tapping day" is a day with latex recorded (> 0). Tapping attendance (trees tapped) is reported alongside.
 * A "rain-lost" day has attendance no_work='rain' or calendar.rained = true.
 */
@Service
@RequiredArgsConstructor
public class WeatherInsightsService {

    private static final BigDecimal RAIN_DAY_MM = BigDecimal.ONE;
    static final String LATEX_UNIT = "litres";

    private final WeatherDailyRepository weatherRepository;
    private final WeatherEstateActivityRepository activityRepository;
    private final WeatherSyncService syncService;

    @Value("${app.weather.tap-rain-threshold-mm:1.0}") private BigDecimal tapRainThresholdMm;

    public WeatherInsightsResponse insights(LocalDate from, LocalDate to) {
        LocalDate today = syncService.today();
        if (to == null) {
            to = today;
        }
        if (from == null) {
            from = YearMonth.from(to).minusMonths(11).atDay(1);
        }
        if (from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }

        Map<LocalDate, WeatherRow> weather = new HashMap<>();
        weatherRepository.findBetween(from, to).forEach(w -> weather.put(w.date(), w));
        Map<LocalDate, BigDecimal> latex = new HashMap<>();
        activityRepository.latexPerDay(from, to).forEach(l -> latex.put(l.date(), l.litres()));
        Map<LocalDate, WeatherEstateActivityRepository.AttendanceDay> attendance = new HashMap<>();
        activityRepository.attendancePerDay(from, to).forEach(a -> attendance.put(a.date(), a));
        Set<LocalDate> rained = new HashSet<>(activityRepository.rainedCalendarDays(from, to));

        // every date that has weather or estate activity
        Set<LocalDate> dates = new HashSet<>(weather.keySet());
        dates.addAll(latex.keySet());
        dates.addAll(attendance.keySet());
        dates.addAll(rained);

        List<Day> days = new ArrayList<>();
        for (LocalDate d : new TreeSet<>(dates)) {
            WeatherRow w = weather.get(d);
            BigDecimal qty = latex.get(d);
            var att = attendance.get(d);
            int tappers = att == null ? 0 : att.tappers();
            long trees = att == null ? 0 : att.trees();
            boolean tapped = (qty != null && qty.signum() > 0) || tappers > 0;
            boolean rainLost = rained.contains(d) || (att != null && att.rainAttendance());
            days.add(new Day(d, w == null ? null : w.precipitationMm(), w == null ? null : w.morningRainMm(),
                    qty, tappers, (int) trees, tapped, rainLost));
        }

        return new WeatherInsightsResponse(
                from, to, LATEX_UNIT, days,
                months(days),
                correlations(days),
                wetDry(days),
                yearToDate(to),
                rainLostThisMonth(days, to),
                YearMonth.from(to).toString());
    }

    private static boolean hasYield(Day d) {
        return d.latexQty() != null && d.latexQty().signum() > 0;
    }

    private List<Month> months(List<Day> days) {
        Map<YearMonth, List<Day>> byMonth = new TreeMap<>();
        for (Day d : days) {
            byMonth.computeIfAbsent(YearMonth.from(d.date()), k -> new ArrayList<>()).add(d);
        }
        List<Month> out = new ArrayList<>();
        byMonth.forEach((ym, list) -> {
            BigDecimal rain = BigDecimal.ZERO;
            boolean anyRain = false;
            int rainDays = 0;
            int tappingDays = 0;
            int lost = 0;
            BigDecimal qty = BigDecimal.ZERO;
            long trees = 0;
            for (Day d : list) {
                if (d.rainfallMm() != null) {
                    anyRain = true;
                    rain = rain.add(d.rainfallMm());
                    if (d.rainfallMm().compareTo(RAIN_DAY_MM) >= 0) {
                        rainDays++;
                    }
                }
                if (hasYield(d)) {
                    tappingDays++;
                    qty = qty.add(d.latexQty());
                }
                if (d.rainLost()) {
                    lost++;
                }
                trees += d.treesTapped();
            }
            BigDecimal avg = tappingDays == 0 ? null
                    : qty.divide(BigDecimal.valueOf(tappingDays), 2, RoundingMode.HALF_UP);
            out.add(new Month(ym.toString(), anyRain ? rain.setScale(1, RoundingMode.HALF_UP) : null, rainDays,
                    tappingDays, lost, qty.setScale(2, RoundingMode.HALF_UP), avg, trees));
        });
        return out;
    }

    private Correlations correlations(List<Day> days) {
        // 1) daily rainfall vs daily yield, over days that have both weather and a recorded yield
        List<double[]> a = new ArrayList<>();
        for (Day d : days) {
            if (d.rainfallMm() != null && hasYield(d)) {
                a.add(new double[]{d.rainfallMm().doubleValue(), d.latexQty().doubleValue()});
            }
        }
        // 2) morning rain vs tapping happening (1/0), over "workable" days: tapped or lost to rain
        List<double[]> b = new ArrayList<>();
        for (Day d : days) {
            if (d.morningRainMm() != null && (d.tapped() || d.rainLost())) {
                b.add(new double[]{d.morningRainMm().doubleValue(), d.tapped() && !d.rainLost() ? 1 : 0});
            }
        }
        return new Correlations(pearson(a), a.size(), pearson(b), b.size(),
                "Days with weather data and latex recorded (litres)",
                "Days with weather data where tapping happened or work was lost to rain; tapped = 1, lost = 0");
    }

    /** Pearson r rounded to 3 dp, or null when n < 3 or either series has no variance. */
    static BigDecimal pearson(List<double[]> pairs) {
        int n = pairs.size();
        if (n < 3) {
            return null;
        }
        double mx = 0;
        double my = 0;
        for (double[] p : pairs) {
            mx += p[0];
            my += p[1];
        }
        mx /= n;
        my /= n;
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (double[] p : pairs) {
            sxy += (p[0] - mx) * (p[1] - my);
            sxx += (p[0] - mx) * (p[0] - mx);
            syy += (p[1] - my) * (p[1] - my);
        }
        if (sxx <= 1e-12 || syy <= 1e-12) {
            return null;
        }
        double r = sxy / Math.sqrt(sxx * syy);
        if (Double.isNaN(r) || Double.isInfinite(r)) {
            return null;
        }
        return BigDecimal.valueOf(r).setScale(3, RoundingMode.HALF_UP);
    }

    private WetDryYield wetDry(List<Day> days) {
        BigDecimal dry = BigDecimal.ZERO;
        BigDecimal wet = BigDecimal.ZERO;
        int dryN = 0;
        int wetN = 0;
        for (Day d : days) {
            if (d.morningRainMm() == null || !hasYield(d)) {
                continue;
            }
            if (d.morningRainMm().compareTo(tapRainThresholdMm) < 0) {
                dry = dry.add(d.latexQty());
                dryN++;
            } else {
                wet = wet.add(d.latexQty());
                wetN++;
            }
        }
        return new WetDryYield(tapRainThresholdMm,
                dryN == 0 ? null : dry.divide(BigDecimal.valueOf(dryN), 2, RoundingMode.HALF_UP), dryN,
                wetN == 0 ? null : wet.divide(BigDecimal.valueOf(wetN), 2, RoundingMode.HALF_UP), wetN);
    }

    private YearToDate yearToDate(LocalDate to) {
        LocalDate jan1 = LocalDate.of(to.getYear(), 1, 1);
        LocalDate lastTo = safeMinusYear(to);
        LocalDate lastJan1 = jan1.minusYears(1);
        double[] cur = sumRain(jan1, to);
        double[] prev = sumRain(lastJan1, lastTo);
        int curDays = (int) cur[1];
        int prevDays = (int) prev[1];
        BigDecimal thisYear = curDays == 0 ? null : bd(cur[0]);
        BigDecimal lastYear = prevDays == 0 ? null : bd(prev[0]);
        BigDecimal diff = null;
        BigDecimal pct = null;
        if (thisYear != null && lastYear != null) {
            diff = thisYear.subtract(lastYear);
            if (lastYear.signum() != 0) {
                pct = diff.multiply(BigDecimal.valueOf(100)).divide(lastYear, 1, RoundingMode.HALF_UP);
            }
        }
        return new YearToDate(to.getYear(), thisYear, lastYear, diff, pct, curDays, prevDays);
    }

    private static LocalDate safeMinusYear(LocalDate d) {
        return d.minusYears(1); // Feb 29 maps to Feb 28
    }

    private double[] sumRain(LocalDate from, LocalDate to) {
        double sum = 0;
        int n = 0;
        for (WeatherRow w : weatherRepository.findBetween(from, to)) {
            if (w.precipitationMm() != null) {
                sum += w.precipitationMm().doubleValue();
                n++;
            }
        }
        return new double[]{sum, n};
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP);
    }

    private Integer rainLostThisMonth(List<Day> days, LocalDate to) {
        YearMonth ym = YearMonth.from(to);
        return (int) days.stream().filter(d -> YearMonth.from(d.date()).equals(ym) && d.rainLost()).count();
    }

    /** Average yield per tapping day over a window, used by the outlook. Returns {avg|null, sampleDays}. */
    public YieldAverage averageYield(LocalDate from, LocalDate to) {
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        for (var l : activityRepository.latexPerDay(from, to)) {
            if (l.litres() != null && l.litres().signum() > 0) {
                sum = sum.add(l.litres());
                n++;
            }
        }
        return new YieldAverage(n == 0 ? null : sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP), n);
    }

    public record YieldAverage(BigDecimal average, int sampleDays) {
    }
}
