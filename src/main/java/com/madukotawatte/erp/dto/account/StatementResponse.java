package com.madukotawatte.erp.dto.account;

import com.madukotawatte.erp.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementResponse {
    private String accountCode;
    private String name;
    private AccountType type;
    private LocalDate from;
    private LocalDate to;
    /** Balance at the end of the day before `from`. */
    private BigDecimal openingBalance;
    private BigDecimal totalIn;
    private BigDecimal totalOut;
    /** Balance at the end of `to` (always the true balance, even when a category filter hides rows). */
    private BigDecimal closingBalance;
    private List<LedgerEntryResponse> rows;
}
