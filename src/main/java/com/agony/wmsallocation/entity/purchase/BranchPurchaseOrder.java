package com.agony.wmsallocation.entity.purchase;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 營業所訂貨單 (BPO)：庫務彙總所屬業務員訂單後，統一向工廠端訂貨的單頭。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/purchase/BranchPurchase.md}。
 * <p>按工廠分單：同一營業所同一訂貨日可能因產品分屬不同工廠而產生多張 BPO。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "branch_purchase_order",
        uniqueConstraints = @UniqueConstraint(columnNames = {"branch_code", "factory_code", "purchase_date"}))
public class BranchPurchaseOrder extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(unique = true, length = 30, nullable = false)
    private String bpoNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String factoryCode;

    @NotNull
    @Column(nullable = false)
    private LocalDate purchaseDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BpoStatus status;
}
