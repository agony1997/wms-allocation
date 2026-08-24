package com.agony.wmsallocation.entity.inventory;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.inventory.enums.InventoryTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 庫存異動流水帳：記錄每筆庫存異動，供追溯來源單據與交叉驗證餘額。
 *
 * <p>三表設計之一（餘額 / 異動 / 快照），業務規則詳見
 * {@code docs/requirements/specification/master/Inventory.md}。
 * <p>注意：寄庫 / 領回寄庫 / 退庫會同時影響兩個儲位，一次業務操作會產生 2 筆記錄。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "inventory_transaction")
public class InventoryTransaction extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

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
    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private InventoryTransactionType transactionType;

    /** qty 的變化量，帶正負號（入庫為 +、出庫為 -）。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer qtyChange = 0;

    /** keepQty 的變化量，帶正負號。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer keepQtyChange = 0;

    /** returnQty 的變化量，帶正負號。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer returnQtyChange = 0;

    /** 來源單據類型（如 FDO、AO、SRO）。 */
    @NotNull
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    private String sourceDocType;

    /** 來源單據號碼。 */
    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String sourceDocNo;
}
