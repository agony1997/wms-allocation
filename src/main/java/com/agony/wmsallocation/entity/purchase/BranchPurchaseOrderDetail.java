package com.agony.wmsallocation.entity.purchase;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 營業所訂貨單明細 (BPOD)：BPO 的逐項產品數量，由彙總同產品的多筆 SPOD 而來。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/purchase/BranchPurchase.md}。
 * <p>唯一鍵：(bpoNo, productCode, unit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "branch_purchase_order_detail", uniqueConstraints = @UniqueConstraint(columnNames = {"bpo_no", "product_code", "unit"}))
public class BranchPurchaseOrderDetail extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String bpoNo;

    @NotNull
    @Column(nullable = false)
    private Integer itemNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String productCode;

    @Nationalized
    @NotNull
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String productName;

    @Nationalized
    @NotNull
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    private String unit;

    /** 彙總而來，非人工輸入：等於同營業所同訂貨日該產品所有 SPOD.confirmedQty 的加總。 */
    @NotNull
    @Column(nullable = false)
    private Integer qty;
}
