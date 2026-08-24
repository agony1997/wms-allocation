package com.agony.wmsallocation.entity.inventory;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 每日庫存快照：每日結算時保存 Inventory 的當日切片，供報表與歷史庫存查詢。
 *
 * <p>三表設計之一（餘額 / 異動 / 快照），業務規則詳見
 * {@code docs/requirements/specification/master/Inventory.md}。
 * <p>唯一鍵：(snapshotDate, branchCode, locationCode, productCode, batchNo)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "inventory_daily_snapshot", uniqueConstraints = @UniqueConstraint(
        columnNames = {"snapshot_date", "branch_code", "location_code", "product_code", "batch_no"}))
public class InventoryDailySnapshot extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(nullable = false)
    private LocalDate snapshotDate;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    /** WAREHOUSE 時 = branchCode；CAR 時為業務員儲位代碼（如 S001）。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private LocationType locationType;

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

    @NotNull
    @Column(nullable = false)
    private Integer qty;

    /** 寄庫數量：僅 WAREHOUSE 有值，CAR 固定 0。 */
    @NotNull
    @Column(nullable = false)
    private Integer keepQty;

    /** 待退庫數量：僅 WAREHOUSE 有值，CAR 固定 0。 */
    @NotNull
    @Column(nullable = false)
    private Integer returnQty;
}
