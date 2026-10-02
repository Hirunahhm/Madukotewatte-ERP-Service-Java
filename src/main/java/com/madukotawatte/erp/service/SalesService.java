package com.madukotawatte.erp.service;

import com.madukotawatte.erp.dto.common.PageResponse;
import com.madukotawatte.erp.dto.finance.CategoryTotalResponse;
import com.madukotawatte.erp.dto.finance.TrendPointResponse;
import com.madukotawatte.erp.dto.sales.SalesBananaRequest;
import com.madukotawatte.erp.dto.sales.SalesBananaResponse;
import com.madukotawatte.erp.dto.sales.SalesCoconutRequest;
import com.madukotawatte.erp.dto.sales.SalesCoconutResponse;
import com.madukotawatte.erp.dto.sales.SalesGeneralRequest;
import com.madukotawatte.erp.dto.sales.SalesGeneralResponse;
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
import com.madukotawatte.erp.entity.SalesGeneral;
import com.madukotawatte.erp.entity.SalesLatex;
import com.madukotawatte.erp.entity.SalesManioc;
import com.madukotawatte.erp.entity.SalesRubberSolid;
import com.madukotawatte.erp.exception.BadRequestException;
import com.madukotawatte.erp.exception.ResourceNotFoundException;
import com.madukotawatte.erp.mapper.SalesBananaMapper;
import com.madukotawatte.erp.mapper.SalesCoconutMapper;
import com.madukotawatte.erp.mapper.SalesGeneralMapper;
import com.madukotawatte.erp.mapper.SalesLatexMapper;
import com.madukotawatte.erp.mapper.SalesManiocMapper;
import com.madukotawatte.erp.mapper.SalesRubberSolidMapper;
import com.madukotawatte.erp.repository.LoadRepository;
import com.madukotawatte.erp.repository.SalesBananaRepository;
import com.madukotawatte.erp.repository.SalesCoconutRepository;
import com.madukotawatte.erp.repository.SalesGeneralRepository;
import com.madukotawatte.erp.repository.SalesLatexRepository;
import com.madukotawatte.erp.repository.SalesLedgerRepository;
import com.madukotawatte.erp.repository.SalesManiocRepository;
import com.madukotawatte.erp.repository.SalesRubberSolidRepository;
import com.madukotawatte.erp.service.support.ProductCatalog;
import com.madukotawatte.erp.service.support.TrendSupport;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    public static final String REF_GENERAL = "SALE_GENERAL";

    private final SalesLatexRepository salesLatexRepository;
    private final SalesRubberSolidRepository salesRubberSolidRepository;
    private final SalesManiocRepository salesManiocRepository;
    private final SalesCoconutRepository salesCoconutRepository;
    private final SalesBananaRepository salesBananaRepository;
    private final SalesGeneralRepository salesGeneralRepository;
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

    // ── Edit / delete / undo payment (all sale types) ───────────────────────────
    private void assertEditable(Boolean paid) {
        if (Boolean.TRUE.equals(paid)) {
            throw new BadRequestException("This sale is already paid. Undo the payment first, then edit it");
        }
    }

    private void assertDeletable(Boolean paid) {
        if (Boolean.TRUE.equals(paid)) {
            throw new BadRequestException("This sale is already paid. Undo the payment first, then delete it");
        }
    }

    private void assertPaidForUndo(Boolean paid) {
        if (!Boolean.TRUE.equals(paid)) {
            throw new BadRequestException("This sale has no payment to undo");
        }
    }

    private void reverseReceipt(MonetaryAssetTransaction tx) {
        if (tx != null) {
            ledgerService.reverseIfActive(tx, "Sale payment undone");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private SalesLatex findLatex(String id) {
        return salesLatexRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalesLatex", "id", id));
    }

    private SalesRubberSolid findRubber(String id) {
        return salesRubberSolidRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalesRubberSolid", "id", id));
    }

    private SalesManioc findManioc(String id) {
        return salesManiocRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalesManioc", "id", id));
    }

    private SalesCoconut findCoconut(String id) {
        return salesCoconutRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalesCoconut", "id", id));
    }

    private SalesBanana findBanana(String id) {
        return salesBananaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalesBanana", "id", id));
    }

    private SalesGeneral findGeneral(String id) {
        return salesGeneralRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("SalesGeneral", "id", id));
    }

    // latex
    @Transactional
    public SalesLatexResponse updateSalesLatex(String id, SalesLatexRequest request) {
        SalesLatex sale = findLatex(id);
        assertEditable(sale.getIsPaymentReceived());
        sale.setLoad(findLoad(request.getLoadId()));
        sale.setMass(request.getMass());
        sale.setLitres(request.getLitres());
        sale.setMetrolacReading(request.getMetrolacReading());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setTotalAmount(request.getMass().multiply(request.getUnitPrice()));
        sale.setBuyerName(blankToNull(request.getBuyerName()));
        sale.setDueDate(request.getDueDate());
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    @Transactional
    public void deleteSalesLatex(String id) {
        SalesLatex sale = findLatex(id);
        assertDeletable(sale.getIsPaymentReceived());
        salesLatexRepository.delete(sale);
    }

    @Transactional
    public SalesLatexResponse undoSalesLatexPayment(String id) {
        SalesLatex sale = findLatex(id);
        assertPaidForUndo(sale.getIsPaymentReceived());
        reverseReceipt(sale.getTransaction());
        sale.setIsPaymentReceived(false);
        sale.setTransaction(null);
        sale.setAccountCode(null);
        sale.setPaymentDate(null);
        return SalesLatexMapper.toResponse(salesLatexRepository.save(sale));
    }

    // rubber solid
    @Transactional
    public SalesRubberSolidResponse updateSalesRubberSolid(String id, SalesRubberSolidRequest request) {
        SalesRubberSolid sale = findRubber(id);
        assertEditable(sale.getIsPaid());
        sale.setLoad(findLoad(request.getLoadId()));
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setBuyerName(blankToNull(request.getBuyerName()));
        sale.setDueDate(request.getDueDate());
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.save(sale));
    }

    @Transactional
    public void deleteSalesRubberSolid(String id) {
        SalesRubberSolid sale = findRubber(id);
        assertDeletable(sale.getIsPaid());
        salesRubberSolidRepository.delete(sale);
    }

    @Transactional
    public SalesRubberSolidResponse undoSalesRubberSolidPayment(String id) {
        SalesRubberSolid sale = findRubber(id);
        assertPaidForUndo(sale.getIsPaid());
        reverseReceipt(sale.getTransaction());
        sale.setIsPaid(false);
        sale.setStatus("pending");
        sale.setTransaction(null);
        sale.setAccountCode(null);
        sale.setPaymentDate(null);
        return SalesRubberSolidMapper.toResponse(salesRubberSolidRepository.save(sale));
    }

    // manioc
    @Transactional
    public SalesManiocResponse updateSalesManioc(String id, SalesManiocRequest request) {
        SalesManioc sale = findManioc(id);
        assertEditable(sale.getIsPaid());
        sale.setLoad(findLoad(request.getLoadId()));
        sale.setType(request.getType());
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setBuyerName(blankToNull(request.getBuyerName()));
        sale.setDueDate(request.getDueDate());
        return SalesManiocMapper.toResponse(salesManiocRepository.save(sale));
    }

    @Transactional
    public void deleteSalesManioc(String id) {
        SalesManioc sale = findManioc(id);
        assertDeletable(sale.getIsPaid());
        salesManiocRepository.delete(sale);
    }

    @Transactional
    public SalesManiocResponse undoSalesManiocPayment(String id) {
        SalesManioc sale = findManioc(id);
        assertPaidForUndo(sale.getIsPaid());
        reverseReceipt(sale.getTransaction());
        sale.setIsPaid(false);
        sale.setStatus("pending");
        sale.setTransaction(null);
        sale.setAccountCode(null);
        sale.setPaymentDate(null);
        return SalesManiocMapper.toResponse(salesManiocRepository.save(sale));
    }

    // coconut
    @Transactional
    public SalesCoconutResponse updateSalesCoconut(String id, SalesCoconutRequest request) {
        SalesCoconut sale = findCoconut(id);
        assertEditable(sale.getIsPaid());
        sale.setLoad(findLoad(request.getLoadId()));
        sale.setType(request.getType());
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setBuyerName(blankToNull(request.getBuyerName()));
        sale.setDueDate(request.getDueDate());
        return SalesCoconutMapper.toResponse(salesCoconutRepository.save(sale));
    }

    @Transactional
    public void deleteSalesCoconut(String id) {
        SalesCoconut sale = findCoconut(id);
        assertDeletable(sale.getIsPaid());
        salesCoconutRepository.delete(sale);
    }

    @Transactional
    public SalesCoconutResponse undoSalesCoconutPayment(String id) {
        SalesCoconut sale = findCoconut(id);
        assertPaidForUndo(sale.getIsPaid());
        reverseReceipt(sale.getTransaction());
        sale.setIsPaid(false);
        sale.setStatus("pending");
        sale.setTransaction(null);
        sale.setAccountCode(null);
        sale.setPaymentDate(null);
        return SalesCoconutMapper.toResponse(salesCoconutRepository.save(sale));
    }

    // banana
    @Transactional
    public SalesBananaResponse updateSalesBanana(String id, SalesBananaRequest request) {
        SalesBanana sale = findBanana(id);
        assertEditable(sale.getIsPaid());
        sale.setLoad(findLoad(request.getLoadId()));
        sale.setType(request.getType());
        sale.setSaleDate(request.getSaleDate());
        sale.setMass(request.getMass());
        sale.setUnitPrice(request.getUnitPrice());
        sale.setBuyerName(blankToNull(request.getBuyerName()));
        sale.setDueDate(request.getDueDate());
        return SalesBananaMapper.toResponse(salesBananaRepository.save(sale));
    }

    @Transactional
    public void deleteSalesBanana(String id) {
        SalesBanana sale = findBanana(id);
        assertDeletable(sale.getIsPaid());
        salesBananaRepository.delete(sale);
    }

    @Transactional
    public SalesBananaResponse undoSalesBananaPayment(String id) {
        SalesBanana sale = findBanana(id);
        assertPaidForUndo(sale.getIsPaid());
        reverseReceipt(sale.getTransaction());
        sale.setIsPaid(false);
        sale.setStatus("pending");
        sale.setTransaction(null);
        sale.setAccountCode(null);
        sale.setPaymentDate(null);
        return SalesBananaMapper.toResponse(salesBananaRepository.save(sale));
    }

    // ── Sales General (cocoa, pepper, cinnamon, ...) ────────────────────────────
    @Transactional
    public SalesGeneralResponse createSalesGeneral(SalesGeneralRequest request) {
        ProductCatalog.require(ProductCatalog.PRODUCTS, request.getProduct(), "product");
        ProductCatalog.require(ProductCatalog.UNITS, request.getUnit(), "unit");
        SalesGeneral sale = SalesGeneralMapper.toEntity(request);
        if (Boolean.TRUE.equals(request.getIsPaid())) {
            applyPayment(sale, request.getAccountCode(),
                    request.getPaymentDate() != null ? request.getPaymentDate() : sale.getSaleDate());
        }
        return SalesGeneralMapper.toResponse(salesGeneralRepository.save(sale));
    }

    public SalesGeneralResponse getSalesGeneral(String id) {
        return SalesGeneralMapper.toResponse(findGeneral(id));
    }

    public PageResponse<SalesGeneralResponse> getAllSalesGeneral(String product, String buyer, Boolean isPaid,
                                                                 LocalDate from, LocalDate to, Pageable pageable) {
        Specification<SalesGeneral> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (product != null && !product.isBlank()) {
                predicates.add(cb.equal(root.get("product"), product));
            }
            if (buyer != null && !buyer.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("buyerName")), "%" + buyer.trim().toLowerCase() + "%"));
            }
            if (isPaid != null) {
                predicates.add(cb.equal(root.get("isPaid"), isPaid));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("saleDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("saleDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(salesGeneralRepository.findAll(spec, pageable).map(SalesGeneralMapper::toResponse));
    }

    @Transactional
    public SalesGeneralResponse updateSalesGeneral(String id, SalesGeneralRequest request) {
        SalesGeneral sale = findGeneral(id);
        assertEditable(sale.getIsPaid());
        ProductCatalog.require(ProductCatalog.PRODUCTS, request.getProduct(), "product");
        ProductCatalog.require(ProductCatalog.UNITS, request.getUnit(), "unit");
        SalesGeneralMapper.applyFields(sale, request);
        return SalesGeneralMapper.toResponse(salesGeneralRepository.save(sale));
    }

    @Transactional
    public void deleteSalesGeneral(String id) {
        SalesGeneral sale = findGeneral(id);
        assertDeletable(sale.getIsPaid());
        salesGeneralRepository.delete(sale);
    }

    @Transactional
    public SalesGeneralResponse markSalesGeneralPaid(String id, SalesMarkPaidRequest request) {
        SalesGeneral sale = findGeneral(id);
        assertUnpaid(sale.getIsPaid(), sale.getProduct().toLowerCase());
        applyPayment(sale, request.getAccountCode(),
                request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        return SalesGeneralMapper.toResponse(salesGeneralRepository.save(sale));
    }

    @Transactional
    public SalesGeneralResponse undoSalesGeneralPayment(String id) {
        SalesGeneral sale = findGeneral(id);
        assertPaidForUndo(sale.getIsPaid());
        reverseReceipt(sale.getTransaction());
        sale.setIsPaid(false);
        sale.setStatus("pending");
        sale.setTransaction(null);
        sale.setAccountCode(null);
        sale.setPaymentDate(null);
        return SalesGeneralMapper.toResponse(salesGeneralRepository.save(sale));
    }

    private void applyPayment(SalesGeneral sale, String accountCode, LocalDate paidOn) {
        sale.setTransaction(receive(REF_GENERAL, sale.getSaleId(), sale.getTotalAmount(), accountCode, paidOn, sale.getProduct()));
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
                        .saleType((String) r[7])
                        .buyerName((String) r[8])
                        .dueDate(r[9] == null ? null : TrendSupport.toLocalDate(r[9]))
                        .paymentDate(r[10] == null ? null : TrendSupport.toLocalDate(r[10]))
                        .overdue(!"paid".equals(r[5]) && r[9] != null && TrendSupport.toLocalDate(r[9]).isBefore(LocalDate.now()))
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
