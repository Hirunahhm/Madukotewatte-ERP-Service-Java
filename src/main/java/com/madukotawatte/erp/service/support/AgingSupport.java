package com.madukotawatte.erp.service.support;

import com.madukotawatte.erp.dto.finance.AgingBucketResponse;
import com.madukotawatte.erp.dto.finance.PartyTotalResponse;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

/** Bucketing helpers shared by receivables and payables ageing. */
public final class AgingSupport {
    private AgingSupport() {}

    public static final List<String> BUCKETS = List.of("0-30", "31-60", "61-90", "90+");
    public static final String UNSPECIFIED = "Unspecified";

    public static String bucketFor(long days) {
        if (days <= 30) return "0-30";
        if (days <= 60) return "31-60";
        if (days <= 90) return "61-90";
        return "90+";
    }

    public static <T> List<AgingBucketResponse> buckets(List<T> rows, Function<T, String> bucket, Function<T, BigDecimal> amount) {
        Map<String, AgingBucketResponse> map = new LinkedHashMap<>();
        for (String b : BUCKETS) {
            map.put(b, AgingBucketResponse.builder().bucket(b).count(0).total(BigDecimal.ZERO).build());
        }
        for (T row : rows) {
            AgingBucketResponse b = map.get(bucket.apply(row));
            b.setCount(b.getCount() + 1);
            b.setTotal(b.getTotal().add(amount.apply(row)));
        }
        return List.copyOf(map.values());
    }

    public static <T> List<PartyTotalResponse> byParty(List<T> rows, Function<T, String> party, Function<T, BigDecimal> amount,
                                                       Predicate<T> overdue, ToLongFunction<T> days) {
        Map<String, PartyTotalResponse> map = new LinkedHashMap<>();
        for (T row : rows) {
            String raw = party.apply(row);
            String name = raw == null || raw.isBlank() ? UNSPECIFIED : raw;
            PartyTotalResponse p = map.computeIfAbsent(name, n -> PartyTotalResponse.builder()
                    .name(n).count(0).total(BigDecimal.ZERO).overdueTotal(BigDecimal.ZERO).oldestDays(0).build());
            p.setCount(p.getCount() + 1);
            p.setTotal(p.getTotal().add(amount.apply(row)));
            if (overdue.test(row)) p.setOverdueTotal(p.getOverdueTotal().add(amount.apply(row)));
            p.setOldestDays(Math.max(p.getOldestDays(), days.applyAsLong(row)));
        }
        return map.values().stream()
                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal()))
                .toList();
    }
}
