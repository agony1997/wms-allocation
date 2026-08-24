package com.agony.wmsallocation.entity.purchase;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 業務員訂貨單 (SPO)：營業員向所屬營業所訂貨的單頭，每儲位每訂貨日僅一筆。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/purchase/SalesPurchase.md}。
 * <p>SPO 本身無狀態欄位：編輯權限由 BPF（營業所凍結單）控制，彙總進度由 SPOD.status 追蹤。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_purchase_order")
public class SalesPurchaseOrder extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false, unique = true)
    private String purchaseNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @NotNull
    @Column(nullable = false)
    private LocalDate purchaseDate;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String purchaseUser;
}
