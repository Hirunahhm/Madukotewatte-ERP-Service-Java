package com.madukotawatte.erp.controller;

import com.madukotawatte.erp.dto.account.AccountBalanceResponse;
import com.madukotawatte.erp.dto.account.AccountCreateRequest;
import com.madukotawatte.erp.dto.account.AccountResponse;
import com.madukotawatte.erp.dto.account.AccountTrendPointResponse;
import com.madukotawatte.erp.dto.account.AccountUpdateRequest;
import com.madukotawatte.erp.dto.account.LedgerEntryResponse;
import com.madukotawatte.erp.dto.account.ManualEntryRequest;
import com.madukotawatte.erp.dto.account.ReverseRequest;
import com.madukotawatte.erp.dto.account.StatementResponse;
import com.madukotawatte.erp.dto.account.TransferRequest;
import com.madukotawatte.erp.dto.account.TransferResponse;
import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.mapper.LedgerEntryMapper;
import com.madukotawatte.erp.service.AccountService;
import com.madukotawatte.erp.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final LedgerService ledgerService;

    // ── Reads: ADMIN + SUPERVISOR ──────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<AccountResponse>> getAccounts() {
        return ResponseEntity.ok(accountService.getAccounts());
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<AccountBalanceResponse>> getBalances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return ResponseEntity.ok(ledgerService.getBalances(asOf));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<List<AccountTrendPointResponse>> getTrend(@RequestParam(defaultValue = "month") String scale) {
        return ResponseEntity.ok(ledgerService.getTrend(scale));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<PageResponse<LedgerEntryResponse>> getLedger(
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) LedgerCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ledgerService.getLedger(accountCode, category, from, to, page, size));
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String code) {
        return ResponseEntity.ok(accountService.getAccount(code));
    }

    @GetMapping("/{code}/statement")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<StatementResponse> getStatement(
            @PathVariable String code,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) LedgerCategory category) {
        return ResponseEntity.ok(ledgerService.getStatement(code, from, to, category));
    }

    // ── Writes: ADMIN only ─────────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }

    @PutMapping("/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable String code, @Valid @RequestBody AccountUpdateRequest request) {
        return ResponseEntity.ok(accountService.updateAccount(code, request));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.transfer(
                request.getFromAccountCode(), request.getToAccountCode(), request.getAmount(),
                request.getDate(), request.getDescription()));
    }

    @PostMapping("/{code}/entries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LedgerEntryResponse> createManualEntry(
            @PathVariable String code, @Valid @RequestBody ManualEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createManualEntry(code, request));
    }

    /**
     * Reverses a manual entry or a transfer. Rows owned by a business record (a sale receipt, an expense payment,
     * a salary payment, ...) must be undone from that record so it is not left orphaned.
     */
    @PostMapping("/ledger/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LedgerEntryResponse> reverse(
            @PathVariable String id, @RequestBody(required = false) ReverseRequest request) {
        LedgerEntryResponse target = ledgerService.getEntry(id);
        if (!Boolean.TRUE.equals(target.getReversible())) {
            if (Boolean.TRUE.equals(target.getReversed()) || target.getReversalOfId() != null) {
                throw new BadRequestException("This entry has already been reversed or is itself a reversal");
            }
            throw new BadRequestException("This entry belongs to " + target.getReferenceType()
                    + " " + target.getReferenceId() + "; undo it from there");
        }
        MonetaryAssetTransaction reversal = ledgerService.reverse(id, request != null ? request.getReason() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LedgerEntryMapper.toResponse(reversal, false, reversal.getNewAmount()));
    }
}
