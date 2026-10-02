package com.madukotawatte.erp.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/** PAID payslips with payment_date in [from, to], per employee and in total. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSummaryResponse {
    private LocalDate from;
    private LocalDate to;
    private List<PayrollSummaryRow> rows;
    private PayrollSummaryRow totals;
}
