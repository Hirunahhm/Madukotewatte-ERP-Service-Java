package com.madukotawatte.erp.dto.account;

import com.madukotawatte.erp.entity.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private String code;
    private String name;
    private AccountType type;
    private String institution;
    private String accountNumber;
    private BigDecimal creditLimit;
    private BigDecimal openingBalance;
    private LocalDate openingDate;
    private Boolean isActive;
    /** Current balance; negative when a credit card owes money. */
    private BigDecimal balance;
    /** Credit cards only: creditLimit + balance. */
    private BigDecimal availableCredit;
    private LocalDateTime createdAt;
}
