package com.madukotawatte.erp.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountTrendPointResponse {
    private String name;
    /** Cash + bank balance at the end of the bucket. */
    private BigDecimal cashAndBank;
    /** Credit-card balance at the end of the bucket (negative = owed). */
    private BigDecimal creditCards;
    /** cashAndBank + creditCards. */
    private BigDecimal net;
}
