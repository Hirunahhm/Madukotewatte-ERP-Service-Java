package com.madukotawatte.erp.dto.account;

import com.madukotawatte.erp.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AccountCreateRequest {
    @NotBlank
    @Size(max = 40)
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-]*$", message = "code may contain letters, digits and hyphens only")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @Size(max = 100)
    private String institution;

    @Size(max = 50)
    private String accountNumber;

    /** Required for CREDIT_CARD, must be absent otherwise. */
    @PositiveOrZero
    private BigDecimal creditLimit;

    /** Signed balance on openingDate. Cards: zero or negative (negative = amount owed). Defaults to 0. */
    private BigDecimal openingBalance;

    /** Defaults to today. */
    private LocalDate openingDate;
}
