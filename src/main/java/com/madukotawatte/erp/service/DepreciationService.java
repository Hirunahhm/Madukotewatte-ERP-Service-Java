package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.fixedasset.DepreciationRowResponse;
import com.madukotawatte.erp.dto.fixedasset.DepreciationScheduleResponse;
import com.madukotawatte.erp.dto.fixedasset.FixedAssetPosition;
import com.madukotawatte.erp.entity.FixedAsset;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed-asset and depreciation schedule (computed, never stored). See {@link DepreciationCalculator} for the
 * month convention. Reused by the reports wave: {@link #schedule(LocalDate, LocalDate)},
 * {@link #nbvAsOf(LocalDate)} and {@link #positionAsOf(LocalDate)}.
 */
@Service
@RequiredArgsConstructor
public class DepreciationService {

    private final FixedAssetRepository fixedAssetRepository;

    /** Per asset: opening/closing cost, accumulated depreciation and NBV, the period charge, additions and disposals. */
    @Transactional(readOnly = true)
    public DepreciationScheduleResponse schedule(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("A valid from/to period is required");
        }
        LocalDate dayBefore = from.minusDays(1);
        List<DepreciationRowResponse> rows = new ArrayList<>();
        for (FixedAsset a : fixedAssetRepository.findAll()) {
            if (a.getAcquisitionDate().isAfter(to)) continue;
            if (a.getDisposalDate() != null && a.getDisposalDate().isBefore(from)) continue;
            rows.add(row(a, from, to, dayBefore));
        }
        rows.sort(Comparator.comparing(DepreciationRowResponse::getCategory)
                .thenComparing(DepreciationRowResponse::getAcquisitionDate)
                .thenComparing(DepreciationRowResponse::getName));

        Map<String, DepreciationRowResponse> byCategory = new LinkedHashMap<>();
        for (DepreciationRowResponse r : rows) {
            byCategory.merge(r.getCategory(), sumInto(null, r, r.getCategory()), (x, y) -> sumInto(x, y, x.getCategory()));
        }
        DepreciationRowResponse total = null;
        for (DepreciationRowResponse c : byCategory.values()) {
            total = sumInto(total, c, "Total");
        }
        if (total == null) {
            total = sumInto(null, emptyRow(), "Total");
        }
        return DepreciationScheduleResponse.builder()
                .from(from).to(to)
                .assets(rows)
                .categories(new ArrayList<>(byCategory.values()))
                .total(total)
                .build();
    }

    /** Fixed-asset position (cost, accumulated depreciation, NBV) of assets on the books on the date. */
    @Transactional(readOnly = true)
    public FixedAssetPosition positionAsOf(LocalDate date) {
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal acc = BigDecimal.ZERO;
        for (FixedAsset a : fixedAssetRepository.findAll()) {
            if (DepreciationCalculator.heldAt(a, date)) {
                cost = cost.add(a.getAcquisitionValue());
                acc = acc.add(DepreciationCalculator.accumulatedThrough(a, date));
            }
        }
        return new FixedAssetPosition(date, cost, acc, cost.subtract(acc));
    }

    /** Total net book value of the fixed assets on the books at the date. */
    @Transactional(readOnly = true)
    public BigDecimal nbvAsOf(LocalDate date) {
        return positionAsOf(date).netBookValue();
    }

    private DepreciationRowResponse row(FixedAsset a, LocalDate from, LocalDate to, LocalDate dayBefore) {
        boolean heldAtStart = DepreciationCalculator.heldAt(a, dayBefore);
        boolean heldAtEnd = DepreciationCalculator.heldAt(a, to);
        boolean acquiredInPeriod = !a.getAcquisitionDate().isBefore(from);
        boolean disposedInPeriod = a.getDisposalDate() != null
                && !a.getDisposalDate().isBefore(from) && !a.getDisposalDate().isAfter(to);

        BigDecimal cost = a.getAcquisitionValue();
        BigDecimal costStart = heldAtStart ? cost : BigDecimal.ZERO;
        BigDecimal accStart = heldAtStart ? DepreciationCalculator.accumulatedThrough(a, dayBefore) : BigDecimal.ZERO;
        BigDecimal accBeforeRemoval = DepreciationCalculator.accumulatedThrough(a, to);
        BigDecimal charge = accBeforeRemoval.subtract(accStart);
        BigDecimal removedAcc = disposedInPeriod ? accBeforeRemoval : BigDecimal.ZERO;
        BigDecimal accEnd = heldAtEnd ? accBeforeRemoval : BigDecimal.ZERO;
        BigDecimal costEnd = heldAtEnd ? cost : BigDecimal.ZERO;

        BigDecimal proceeds = BigDecimal.ZERO;
        BigDecimal gainLoss = BigDecimal.ZERO;
        if (disposedInPeriod) {
            proceeds = a.getDisposalProceeds() != null ? a.getDisposalProceeds() : BigDecimal.ZERO;
            gainLoss = proceeds.subtract(cost.subtract(accBeforeRemoval));
        }
        return DepreciationRowResponse.builder()
                .assetId(a.getAssetId())
                .name(a.getName())
                .category(a.getCategory())
                .acquisitionDate(a.getAcquisitionDate())
                .disposalDate(a.getDisposalDate())
                .depreciationMethod(a.getDepreciationMethod())
                .usefulLifeYears(a.getUsefulLifeYears())
                .cost(cost)
                .costAtStart(costStart)
                .additions(acquiredInPeriod ? cost : BigDecimal.ZERO)
                .disposalCost(disposedInPeriod ? cost : BigDecimal.ZERO)
                .costAtEnd(costEnd)
                .accumulatedAtStart(accStart)
                .chargeForPeriod(charge)
                .accumulatedOnDisposals(removedAcc)
                .accumulatedAtEnd(accEnd)
                .nbvAtStart(costStart.subtract(accStart))
                .nbvAtEnd(costEnd.subtract(accEnd))
                .disposalProceeds(proceeds)
                .disposalGainLoss(gainLoss)
                .build();
    }

    private static DepreciationRowResponse emptyRow() {
        BigDecimal z = BigDecimal.ZERO;
        return DepreciationRowResponse.builder().cost(z).costAtStart(z).additions(z).disposalCost(z).costAtEnd(z)
                .accumulatedAtStart(z).chargeForPeriod(z).accumulatedOnDisposals(z).accumulatedAtEnd(z)
                .nbvAtStart(z).nbvAtEnd(z).disposalProceeds(z).disposalGainLoss(z).build();
    }

    /** Adds {@code r} into {@code acc} (or into a fresh total row when acc is null). */
    private static DepreciationRowResponse sumInto(DepreciationRowResponse acc, DepreciationRowResponse r, String category) {
        DepreciationRowResponse base = acc != null ? acc : emptyRow();
        return DepreciationRowResponse.builder()
                .category(category)
                .cost(base.getCost().add(r.getCost()))
                .costAtStart(base.getCostAtStart().add(r.getCostAtStart()))
                .additions(base.getAdditions().add(r.getAdditions()))
                .disposalCost(base.getDisposalCost().add(r.getDisposalCost()))
                .costAtEnd(base.getCostAtEnd().add(r.getCostAtEnd()))
                .accumulatedAtStart(base.getAccumulatedAtStart().add(r.getAccumulatedAtStart()))
                .chargeForPeriod(base.getChargeForPeriod().add(r.getChargeForPeriod()))
                .accumulatedOnDisposals(base.getAccumulatedOnDisposals().add(r.getAccumulatedOnDisposals()))
                .accumulatedAtEnd(base.getAccumulatedAtEnd().add(r.getAccumulatedAtEnd()))
                .nbvAtStart(base.getNbvAtStart().add(r.getNbvAtStart()))
                .nbvAtEnd(base.getNbvAtEnd().add(r.getNbvAtEnd()))
                .disposalProceeds(base.getDisposalProceeds().add(r.getDisposalProceeds()))
                .disposalGainLoss(base.getDisposalGainLoss().add(r.getDisposalGainLoss()))
                .build();
    }
}
