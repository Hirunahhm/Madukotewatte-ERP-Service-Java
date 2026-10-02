package com.madukotawatte.erp.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    /** Shared reference_id of both legs (reference_type 'TRANSFER'). */
    private String transferId;
    private LedgerEntryResponse from;
    private LedgerEntryResponse to;
}
