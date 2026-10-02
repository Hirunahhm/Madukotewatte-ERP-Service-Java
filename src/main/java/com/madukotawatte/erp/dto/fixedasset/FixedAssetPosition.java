package com.madukotawatte.erp.dto.fixedasset;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fixed-asset position on a date (for the balance sheet). */
public record FixedAssetPosition(LocalDate asOf, BigDecimal cost, BigDecimal accumulatedDepreciation, BigDecimal netBookValue) {}
