package com.madukotawatte.erp.dto.estateloan;

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
public class CreditCardStatementResponse {
    private String loanType;
    private LocalDate from;
    private LocalDate to;
    private BigDecimal openingBalance;
    private BigDecimal totalCharges;
    private BigDecimal totalPayments;
    private BigDecimal closingBalance;
    private List<EstateLoanTransactionResponse> transactions;
}
