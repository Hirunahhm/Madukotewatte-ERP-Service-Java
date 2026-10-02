package com.madukotawatte.erp.dto.estateloan;

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
public class EstateLoanResponse {
    private String loanId;
    private String name;
    /** Total borrowed so far (initial amount plus later drawdowns). */
    private BigDecimal principalAmount;
    private BigDecimal currentBalance;
    private LocalDate issuedDate;
    private BigDecimal interestRate;
    private Boolean isActive;
    private String lender;
    private Integer termMonths;
    private BigDecimal installmentAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
