package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "sales_manioc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SalesManioc extends BaseEntity {

    @Id
    @Column(name = "sale_id", length = 36)
    private String saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "load_id", nullable = false)
    private Load load;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal mass;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "is_paid", nullable = false)
    private Boolean isPaid = false;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "payment_type", length = 50)
    private String paymentType;
}
