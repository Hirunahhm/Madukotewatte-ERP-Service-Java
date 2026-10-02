package com.madukotawatte.erp.dto.account;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Every field is optional; null leaves the current value unchanged. */
@Data
public class AccountUpdateRequest {
    @Size(max = 100)
    private String name;
    @Size(max = 100)
    private String institution;
    @Size(max = 50)
    private String accountNumber;
    private BigDecimal creditLimit;
    private BigDecimal openingBalance;
    private LocalDate openingDate;
    private Boolean isActive;
}
