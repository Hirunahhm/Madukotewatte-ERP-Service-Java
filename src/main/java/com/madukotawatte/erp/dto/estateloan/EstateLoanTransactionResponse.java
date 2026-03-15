package com.madukotawatte.erp.dto.estateloan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstateLoanTransactionResponse {
    private String id;
    private String loanType;
    private BigDecimal lastAmount;
    private BigDecimal newAmount;
    private LocalDateTime createdAt;
}
