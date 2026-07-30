package com.madukotawatte.erp.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "credit_card_limits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreditCardLimit extends BaseEntity {

    @Id
    @Column(name = "loan_type", length = 30)
    private String loanType;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;
}
