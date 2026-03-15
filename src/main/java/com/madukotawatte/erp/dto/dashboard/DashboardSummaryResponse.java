package com.madukotawatte.erp.dto.dashboard;

import com.madukotawatte.erp.dto.monetary.AssetBalanceResponse;
import com.madukotawatte.erp.dto.estateloan.EstateLoanBalanceResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private List<AssetBalanceResponse> assetBalances;
    private List<EstateLoanBalanceResponse> loanBalances;
    private long activeEmployeeLoans;
    private long todayAttendanceCount;
    private long unpaidSalesCount;
}
