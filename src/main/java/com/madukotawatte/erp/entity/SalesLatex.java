package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sales_latex")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesLatex extends BaseEntity {

    @Id
    @Column(name = "sale_id", length = 36)
    private String saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id")
    private Load load;

    @Column(precision = 10, scale = 2)
    private BigDecimal mass;

    @Column(precision = 10, scale = 2)
    private BigDecimal litres;

    @Column(name = "metrolac_reading", precision = 10, scale = 2)
    private BigDecimal metrolacReading;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "is_payment_received")
    private Boolean isPaymentReceived = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;
}
