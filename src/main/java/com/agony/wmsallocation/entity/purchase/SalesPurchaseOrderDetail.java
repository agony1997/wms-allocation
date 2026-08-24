package com.agony.wmsallocation.entity.purchase;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 業務員訂貨單明細 (SPOD)：訂貨單的逐項產品數量，是彙總進 BPO 的最小單位。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/purchase/SalesPurchase.md}。
 * <p>狀態落在明細而非單頭：同一 SPO 的明細可能依產品工廠分進不同 BPO。
 * <p>唯一鍵：(purchaseNo, productCode, unit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_purchase_order_detail", uniqueConstraints = @UniqueConstraint(columnNames = {"purchase_no", "product_code", "unit"}))
public class SalesPurchaseOrderDetail extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(nullable = false, length = 30)
    private String purchaseNo;

    @NotNull
    @Column(nullable = false)
    private int itemNo;

    @NotNull
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String productCode;

    @Nationalized
    @Size(max = 100)
    @Column(length = 100)
    private String productName;

    @Nationalized
    @NotNull
    @Size(max = 5)
    @Column(nullable = false, length = 5)
    private String unit;

    /** 業務員原始訂購數量；組長凍結後不再變動，與 confirmedQty 並存以供差異分析。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int qty;

    /** 組長確認後的數量，建立時預設 = qty；彙總進 BPO 時加總的是此欄位而非 qty。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int confirmedQty;

    @Column(columnDefinition = "integer default 0")
    private int lastQty;

    /** 彙總進度：PENDING（尚未被庫務彙總）→ AGGREGATED（已併入 BPO）→ ALLOCATED（已配貨）。 */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SalesOrderDetailStatus status = SalesOrderDetailStatus.PENDING;

    /** 彙總去向：併入的 BPO 單號；彙總前為 null，彙總時回填，供配貨查詢「對應 BPO 已收貨」。 */
    @Size(max = 30)
    @Column(length = 30)
    private String bpoNo;

    @Column(columnDefinition = "integer default 0")
    private int sortOrder;
}
