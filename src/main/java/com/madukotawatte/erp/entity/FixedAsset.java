package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "fixed_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FixedAsset extends BaseEntity {

    @Id
    @Column(name = "asset_id", length = 36)
    private String assetId;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Column(name = "acquisition_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal acquisitionValue;

    @Column(name = "current_value", precision = 15, scale = 2, nullable = false)
    private BigDecimal currentValue;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "active";

    @Column(length = 255)
    private String location;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "useful_life_years")
    private Integer usefulLifeYears;

    @Column(name = "residual_value", precision = 15, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal residualValue = BigDecimal.ZERO;

    /** STRAIGHT_LINE | NONE */
    @Column(name = "depreciation_method", nullable = false, length = 20)
    @Builder.Default
    private String depreciationMethod = "STRAIGHT_LINE";

    @Column(name = "purchase_account_code", length = 40)
    private String purchaseAccountCode;

    @Column(name = "purchase_transaction_id", length = 36)
    private String purchaseTransactionId;

    @Column(name = "disposal_date")
    private LocalDate disposalDate;

    @Column(name = "disposal_proceeds", precision = 15, scale = 2)
    private BigDecimal disposalProceeds;

    @Column(name = "disposal_account_code", length = 40)
    private String disposalAccountCode;

    @Column(name = "disposal_transaction_id", length = 36)
    private String disposalTransactionId;
}
