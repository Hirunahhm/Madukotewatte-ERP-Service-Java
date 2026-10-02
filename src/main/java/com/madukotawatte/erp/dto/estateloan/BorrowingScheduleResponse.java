package com.madukotawatte.erp.dto.estateloan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowingScheduleResponse {
    private LocalDate from;
    private LocalDate to;
    private List<BorrowingRowResponse> loans;
    /** Sum over all loans (loanId and name null). */
    private BorrowingRowResponse total;
}
