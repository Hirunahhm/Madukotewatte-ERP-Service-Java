package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.finance.ReceivableRowResponse;
import com.madukotawatte.erp.dto.finance.ReceivablesResponse;
import com.madukotawatte.erp.repository.SalesLedgerRepository;
import com.madukotawatte.erp.service.support.AgingSupport;
import com.madukotawatte.erp.service.support.TrendSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Trade receivables: sales not yet paid on a given day, aged from the sale date. Reused by the annual-report notes. */
@Service
@RequiredArgsConstructor
public class ReceivablesService {

    private final SalesLedgerRepository salesLedgerRepository;

    /**
     * A sale counts when sale_date &lt;= asOf and (it is unpaid, or its payment_date is after asOf). Overdue means
     * the due date is before asOf.
     */
    @Transactional(readOnly = true)
    public ReceivablesResponse getReceivables(LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        List<ReceivableRowResponse> rows = salesLedgerRepository.findReceivableRows(date).stream()
                .map(r -> {
                    LocalDate saleDate = TrendSupport.toLocalDate(r[4]);
                    LocalDate dueDate = r[5] == null ? null : TrendSupport.toLocalDate(r[5]);
                    long days = Math.max(0, ChronoUnit.DAYS.between(saleDate, date));
                    return ReceivableRowResponse.builder()
                            .saleType((String) r[0])
                            .saleId((String) r[1])
                            .product((String) r[2])
                            .buyerName((String) r[3])
                            .saleDate(saleDate)
                            .dueDate(dueDate)
                            .amount((BigDecimal) r[6])
                            .daysOutstanding(days)
                            .bucket(AgingSupport.bucketFor(days))
                            .overdue(dueDate != null && dueDate.isBefore(date))
                            .build();
                })
                .toList();
        BigDecimal total = rows.stream().map(ReceivableRowResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal overdue = rows.stream().filter(ReceivableRowResponse::isOverdue)
                .map(ReceivableRowResponse::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return ReceivablesResponse.builder()
                .asOf(date)
                .total(total)
                .overdueTotal(overdue)
                .rows(rows)
                .buckets(AgingSupport.buckets(rows, ReceivableRowResponse::getBucket, ReceivableRowResponse::getAmount))
                .byBuyer(AgingSupport.byParty(rows, ReceivableRowResponse::getBuyerName, ReceivableRowResponse::getAmount,
                        ReceivableRowResponse::isOverdue, ReceivableRowResponse::getDaysOutstanding))
                .build();
    }
}
