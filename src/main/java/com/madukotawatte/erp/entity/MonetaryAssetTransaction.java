package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "monetary_asset_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonetaryAssetTransaction extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    // 'money in' | 'money out'
    @Column(name = "transaction_type", length = 10)
    private String transactionType;

    // 'Cash' | 'Bank-BOC' | 'Bank-Peoples' | 'Bank-Seylan'
    @Column(name = "asset_type", length = 20)
    private String assetType;

    @Column(name = "last_amount", precision = 15, scale = 2)
    private BigDecimal lastAmount;

    @Column(name = "new_amount", precision = 15, scale = 2)
    private BigDecimal newAmount;
}
