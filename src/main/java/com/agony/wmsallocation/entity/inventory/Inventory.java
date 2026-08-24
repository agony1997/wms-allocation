package com.agony.wmsallocation.entity.inventory;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 庫存餘額表：各儲位某商品某批次的即時數量，供業務操作 O(1) 查詢當下庫存。
 *
 * <p>三表設計之一（餘額 / 異動 / 快照），業務規則詳見
 * {@code docs/requirements/specification/master/Inventory.md}。
 * <p>唯一鍵：(branchCode, locationCode, productCode, batchNo)
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "inventory", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_code", "location_code", "product_code", "batch_no"}))
public class Inventory extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @EqualsAndHashCode.Include
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    /** WAREHOUSE 時 = branchCode；CAR 時為業務員儲位代碼（如 S001）。 */
    @EqualsAndHashCode.Include
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private LocationType locationType;

    @EqualsAndHashCode.Include
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String productCode;

    @EqualsAndHashCode.Include
    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String batchNo;

    /**
     * 效期。對 (productCode, batchNo) 為函數相依，是刻意的 denormalization；
     * Service 須保證同一 (productCode, batchNo) 的所有列共用同一 expiryDate。
     */
    @NotNull
    @Column(nullable = false)
    private LocalDate expiryDate;

    @NotNull
    @Column(nullable = false)
    private Integer qty;

    /** 寄庫數量：僅 WAREHOUSE 有值，CAR 固定 0。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer keepQty = 0;

    /** 待退庫數量：僅 WAREHOUSE 有值，CAR 固定 0。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer returnQty = 0;
}
