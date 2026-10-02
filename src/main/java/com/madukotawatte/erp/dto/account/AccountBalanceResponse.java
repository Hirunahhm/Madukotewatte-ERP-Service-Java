package com.madukotawatte.erp.dto.account;

import com.madukotawatte.erp.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceResponse {
    private String accountCode;
    private String name;
    private AccountType type;
    private Boolean isActive;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private BigDecimal availableCredit;
}
