package com.agony.wmsallocation.entity.allocation;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 配貨單明細 (AOD)：一筆「某業務員 × 某商品 × 某批次」的配貨結果。
 *
 * <p>batchNo/expiryDate 為 FIFO 分配的結果，同一商品的不同批次會拆成多筆 AOD。
 * 業務規則詳見 {@code docs/requirements/specification/allocation/AllocationOrder.md}。
 * <p>唯一鍵：(allocationNo, itemNo)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "allocation_order_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"allocation_no", "item_no"}))
public class AllocationOrderDetail extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String allocationNo;

    @NotNull
    @Column(nullable = false)
    private Integer itemNo;

    /** 目標儲位 = 領貨的業務員儲位（CAR），非大庫。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String productCode;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String batchNo;

    @NotNull
    @Column(nullable = false)
    private LocalDate expiryDate;

    /** 預定數量，來自 SPOD.confirmedQty。 */
    @NotNull
    @Column(nullable = false)
    private Integer requestedQty;

    /** 實際配貨數量；庫存不足時可能小於 requestedQty，低優先業務員甚至為 0。 */
    @NotNull
    @Column(nullable = false)
    private Integer allocatedQty = 0;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AllocationStatus status;
}
