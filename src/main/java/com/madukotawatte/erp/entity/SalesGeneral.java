package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Sale of a non-core product (cocoa, pepper, cinnamon, timber, ...). */
@Entity
@Table(name = "sales_general")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesGeneral extends BaseEntity {

    @Id
    @Column(name = "sale_id", length = 36)
    private String saleId;

    @Column(nullable = false, length = 30)
    private String product;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "buyer_name", length = 150)
    private String buyerName;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "account_code", length = 40)
    private String accountCode;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id")
    private MonetaryAssetTransaction transaction;
}
