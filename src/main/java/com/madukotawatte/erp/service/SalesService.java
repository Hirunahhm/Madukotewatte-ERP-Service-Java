package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.finance.TrendPointResponse;
import com.madukotawatte.erp.dto.sales.SalesBananaRequest;
import com.madukotawatte.erp.dto.sales.SalesBananaResponse;
import com.madukotawatte.erp.dto.sales.SalesCoconutRequest;
import com.madukotawatte.erp.dto.sales.SalesCoconutResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexRequest;
import com.madukotawatte.erp.dto.sales.SalesLatexResponse;
import com.madukotawatte.erp.dto.sales.SalesLatexUpdateRequest;
import com.madukotawatte.erp.dto.sales.SalesLedgerRowResponse;
import com.madukotawatte.erp.dto.sales.SalesManiocRequest;
import com.madukotawatte.erp.dto.sales.SalesManiocResponse;
import com.madukotawatte.erp.dto.sales.SalesMarkPaidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidRequest;
import com.madukotawatte.erp.dto.sales.SalesRubberSolidResponse;
import com.madukotawatte.erp.dto.sales.SalesSummaryResponse;
import com.madukotawatte.erp.entity.Load;
import com.madukotawatte.erp.entity.LedgerCategory;
import com.madukotawatte.erp.entity.LedgerDirection;
import com.madukotawatte.erp.entity.MonetaryAssetTransaction;
import com.madukotawatte.erp.entity.SalesBanana;
import com.madukotawatte.erp.entity.SalesCoconut;
import com.madukotawatte.erp.entity.SalesLatex;
import com.madukotawatte.erp.entity.SalesManioc;
import com.madukotawatte.erp.entity.SalesRubberSolid;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.SalesBananaMapper;
import com.madukotawatte.erp.mapper.SalesCoconutMapper;
import com.madukotawatte.erp.mapper.SalesLatexMapper;
import com.madukotawatte.erp.mapper.SalesManiocMapper;
import com.madukotawatte.erp.mapper.SalesRubberSolidMapper;
import com.madukotawatte.erp.repository.LoadRepository;
import com.madukotawatte.erp.repository.SalesBananaRepository;
import com.madukotawatte.erp.repository.SalesCoconutRepository;
import com.madukotawatte.erp.repository.SalesLatexRepository;
import com.madukotawatte.erp.repository.SalesLedgerRepository;
import com.madukotawatte.erp.repository.SalesManiocRepository;
import com.madukotawatte.erp.repository.SalesRubberSolidRepository;
import com.madukotawatte.erp.service.support.TrendSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Sales of every product. A sale is recorded unpaid (revenue + receivable); receiving the money posts a
 * SALE_RECEIPT ledger row into a cash/bank account and stamps the sale with the account and payment date.
 */
@Service
@RequiredArgsConstructor
public class SalesService {

    public static final String REF_LATEX = "SALE_LATEX";
    public static final String REF_RUBBER_SOLID = "SALE_RUBBER_SOLID";
    public static final String REF_MANIOC = "SALE_MANIOC";
    public static final String REF_COCONUT = "SALE_COCONUT";
    public static final String REF_BANANA = "SALE_BANANA";

    private final SalesLatexRepository salesLatexRepository;
    private final SalesRubberSolidRepository salesRubberSolidRepository;
    private final SalesManiocRepository salesManiocRepository;
    private final SalesCoconutRepository salesCoconutRepository;
    private final SalesBananaRepository salesBananaRepository;
    private final SalesLedgerRepository salesLedgerRepository;
    private final LoadRepository loadRepository;
    private final LedgerService ledgerService;

    /** Posts the SALE_RECEIPT row for a sale. */
    private MonetaryAssetTransaction receive(String referenceType, String saleId, BigDecimal total,
                                             String accountCode, LocalDate paidOn, String label) {
        ledgerService.requireCashOrBank(accountCode);
        return ledgerService.post(accountCode, LedgerDirection.IN, total, paidOn, LedgerCategory.SALE_RECEIPT,
                referenceType, saleId, label + " sale payment");
    }

    private Load findLoad(String loadId) {
        return loadRepository.findById(loadId)
                .orElseThrow(() -> new ResourceNotFoundException("Load", "id", loadId));
    }

    private void assertUnpaid(Boolean paid, String label) {
        if (Boolean.TRUE.equals(paid)) {
            throw new BadRequestException("This " + label + " sale is already marked as paid");
        }
    }

    // ── Sales Latex ─────────────────────────────────────────────
    @Transactional
    public SalesLatexResponse createSalesLatex(SalesLatexRequest request) {
        SalesLatex sale = SalesLatexMapper.toEntity(request, findLoad(request.getLoadId()));
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    public SalesLatexResponse getSalesLatex(String id) {
        return SalesLatexMapper.toResponse(salesLatexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id)));
    }

    public PageResponse<SalesLatexResponse> getAllSalesLatex(Pageable pageable) {
        return PageResponse.from(salesLatexRepository.findAll(pageable).map(SalesLatexMapper::toResponse));
    }

    public List<SalesLatexResponse> getUnpaidSales() {
        return salesLatexRepository.findByIsPaymentReceivedFalse().stream()
                .map(SalesLatexMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SalesLatexResponse markPaymentReceived(String id, SalesLatexUpdateRequest request) {
        SalesLatex sale = salesLatexRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id));
        assertUnpaid(sale.getIsPaymentReceived(), "latex");
        LocalDate paidOn = request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now();
        MonetaryAssetTransaction tx = receive(REF_LATEX, sale.getSaleId(), sale.getTotalAmount(),
                request.getAccountCode(), paidOn, "Latex");
        sale.setIsPaymentReceived(true);
        sale.setAccountCode(request.getAccountCode());
        sale.setPaymentDate(paidOn);
        sale.setTransaction(tx);
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    // ── Sales Rubber Solid ───────────────────────────────────────
    @Transactional
    public SalesRubberSolidResponse createSalesRubberSolid(SalesRubberSolidRequest request) {
        SalesRubberSolid sale = SalesRubberSolidMapper.toEntity(request, findLoad(request.getLoadId()));
        if (Boolean.TRUE.equals(request.getIsPaid())) {
            applyPayment(sale, request.getAccountCode(),
                    request.getPaymentDate() != null ? request.getPaymentDate() : sale.getSaleDate());
        }
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.save(sale));
    }

    public SalesRubberSolidResponse getSalesRubberSolid(String id) {
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesRubberSolid", "id", id)));
    }

    public PageResponse<SalesRubberSolidResponse> getAllSalesRubberSolid(Pageable pageable) {
        return PageResponse.from(salesRubberSolidRepository.findAll(pageable).map(SalesRubberSolidMapper::toResponse));
    }

    @Transactional
    public SalesRubberSolidResponse markSalesRubberSolidPaid(String id, SalesMarkPaidRequest request) {
        SalesRubberSolid sale = salesRubberSolidRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesRubberSolid", "id", id));
        assertUnpaid(sale.getIsPaid(), "rubber solid");
        applyPayment(sale, request.getAccountCode(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.save(sale));
    }

    private void applyPayment(SalesRubberSolid sale, String accountCode, LocalDate paidOn) {
        sale.setTransaction(receive(REF_RUBBER_SOLID, sale.getSaleId(), sale.getMass().multiply(sale.getUnitPrice()),
                accountCode, paidOn, "Rubber solid"));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setAccountCode(accountCode);
        sale.setPaymentDate(paidOn);
    }

    // ── Sales Manioc ─────────────────────────────────────────────
    @Transactional
    public SalesManiocResponse createSalesManioc(SalesManiocRequest request) {
        SalesManioc sale = SalesManiocMapper.toEntity(request, findLoad(request.getLoadId()));
        if (Boolean.TRUE.equals(request.getIsPaid())) {
            applyPayment(sale, request.getAccountCode(),
                    request.getPaymentDate() != null ? request.getPaymentDate() : sale.getSaleDate());
        }
        return SalesManiocMapper.toResponse(salesManiocRepository.save(sale));
    }

    public SalesManiocResponse getSalesManioc(String id) {
        return SalesManiocMapper.toResponse(salesManiocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesManioc", "id", id)));
    }

    public PageResponse<SalesManiocResponse> getAllSalesManioc(Pageable pageable) {
        return PageResponse.from(salesManiocRepository.findAll(pageable).map(SalesManiocMapper::toResponse));
    }

    @Transactional
    public SalesManiocResponse markSalesManiocPaid(String id, SalesMarkPaidRequest request) {
        SalesManioc sale = salesManiocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesManioc", "id", id));
        assertUnpaid(sale.getIsPaid(), "manioc");
        applyPayment(sale, request.getAccountCode(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        return SalesManiocMapper.toResponse(salesManiocRepository.save(sale));
    }

    private void applyPayment(SalesManioc sale, String accountCode, LocalDate paidOn) {
        sale.setTransaction(receive(REF_MANIOC, sale.getSaleId(), sale.getMass().multiply(sale.getUnitPrice()),
                accountCode, paidOn, "Manioc"));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setAccountCode(accountCode);
        sale.setPaymentDate(paidOn);
    }

    // ── Sales Coconut ────────────────────────────────────────────
    @Transactional
    public SalesCoconutResponse createSalesCoconut(SalesCoconutRequest request) {
        SalesCoconut sale = SalesCoconutMapper.toEntity(request, findLoad(request.getLoadId()));
        if (Boolean.TRUE.equals(request.getIsPaid())) {
            applyPayment(sale, request.getAccountCode(),
                    request.getPaymentDate() != null ? request.getPaymentDate() : sale.getSaleDate());
        }
        return SalesCoconutMapper.toResponse(salesCoconutRepository.save(sale));
    }

    public SalesCoconutResponse getSalesCoconut(String id) {
        return SalesCoconutMapper.toResponse(salesCoconutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesCoconut", "id", id)));
    }

    public PageResponse<SalesCoconutResponse> getAllSalesCoconut(Pageable pageable) {
        return PageResponse.from(salesCoconutRepository.findAll(pageable).map(SalesCoconutMapper::toResponse));
    }

    @Transactional
    public SalesCoconutResponse markSalesCoconutPaid(String id, SalesMarkPaidRequest request) {
        SalesCoconut sale = salesCoconutRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesCoconut", "id", id));
        assertUnpaid(sale.getIsPaid(), "coconut");
        applyPayment(sale, request.getAccountCode(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        return SalesCoconutMapper.toResponse(salesCoconutRepository.save(sale));
    }

    private void applyPayment(SalesCoconut sale, String accountCode, LocalDate paidOn) {
        sale.setTransaction(receive(REF_COCONUT, sale.getSaleId(), sale.getMass().multiply(sale.getUnitPrice()),
                accountCode, paidOn, "Coconut"));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setAccountCode(accountCode);
        sale.setPaymentDate(paidOn);
    }

    // ── Sales Banana ─────────────────────────────────────────────
    @Transactional
    public SalesBananaResponse createSalesBanana(SalesBananaRequest request) {
        SalesBanana sale = SalesBananaMapper.toEntity(request, findLoad(request.getLoadId()));
        if (Boolean.TRUE.equals(request.getIsPaid())) {
            applyPayment(sale, request.getAccountCode(),
                    request.getPaymentDate() != null ? request.getPaymentDate() : sale.getSaleDate());
        }
        return SalesBananaMapper.toResponse(salesBananaRepository.save(sale));
    }

    public SalesBananaResponse getSalesBanana(String id) {
        return SalesBananaMapper.toResponse(salesBananaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesBanana", "id", id)));
    }

    public PageResponse<SalesBananaResponse> getAllSalesBanana(Pageable pageable) {
        return PageResponse.from(salesBananaRepository.findAll(pageable).map(SalesBananaMapper::toResponse));
    }

    @Transactional
    public SalesBananaResponse markSalesBananaPaid(String id, SalesMarkPaidRequest request) {
        SalesBanana sale = salesBananaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SalesBanana", "id", id));
        assertUnpaid(sale.getIsPaid(), "banana");
        applyPayment(sale, request.getAccountCode(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        return SalesBananaMapper.toResponse(salesBananaRepository.save(sale));
    }

    private void applyPayment(SalesBanana sale, String accountCode, LocalDate paidOn) {
        sale.setTransaction(receive(REF_BANANA, sale.getSaleId(), sale.getMass().multiply(sale.getUnitPrice()),
                accountCode, paidOn, "Banana"));
        sale.setIsPaid(true);
        sale.setStatus("paid");
        sale.setAccountCode(accountCode);
        sale.setPaymentDate(paidOn);
    }

    // ── Sales Ledger + Stats (cross-category) ───────────────────
    public PageResponse<SalesLedgerRowResponse> getSalesLedger(String category, String status, LocalDate from, LocalDate to, Pageable pageable) {
        List<Object[]> rows = salesLedgerRepository.findLedgerRows(category, status, from, to, pageable.getPageSize(), (int) pageable.getOffset());
        long total = salesLedgerRepository.countLedgerRows(category, status, from, to);
        List<SalesLedgerRowResponse> content = rows.stream()
                .map(r -> SalesLedgerRowResponse.builder()
                        .saleId((String) r[0])
                        .category((String) r[1])
                        .loadId((String) r[2])
                        .saleDate(TrendSupport.toLocalDate(r[3]))
                        .amount((BigDecimal) r[4])
                        .status((String) r[5])
                        .accountCode((String) r[6])
                        .build())
                .collect(Collectors.toList());
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil((double) total / pageable.getPageSize());
        return PageResponse.<SalesLedgerRowResponse>builder()
                .content(content)
                .pageNumber(pageable.getPageNumber())
                .pageSize(pageable.getPageSize())
                .totalElements(total)
                .totalPages(totalPages)
                .last(pageable.getPageNumber() >= totalPages - 1)
                .build();
    }

    public SalesSummaryResponse getSalesSummary(LocalDate from, LocalDate to) {
        Object[] row = salesLedgerRepository.sumByPaidStatus(from, to);
        BigDecimal received = (BigDecimal) row[0];
        BigDecimal pending = (BigDecimal) row[1];
        return SalesSummaryResponse.builder()
                .totalSales(received.add(pending))
                .received(received)
                .pending(pending)
                .build();
    }

    public List<CategoryTotalResponse> getSalesDistribution(LocalDate from, LocalDate to) {
        return salesLedgerRepository.sumByCategory(from, to).stream()
                .map(r -> CategoryTotalResponse.builder().category((String) r[0]).total((BigDecimal) r[1]).build())
                .collect(Collectors.toList());
    }

    public List<TrendPointResponse> getSalesTrend(String scale) {
        TrendSupport.Window window = TrendSupport.resolveWindow(scale);
        return TrendSupport.bucketSums(scale, salesLedgerRepository.findAmountsInRange(window.from(), window.to()));
    }
}
