package com.agony.wmsallocation.entity.purchase;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 營業所凍結單 (BPF)：以「營業所 + 訂貨日」為粒度控制當天所有 SPO 的編輯權限。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/purchase/BranchPurchase.md}。
 * <p>權限三階段：BPF 不存在＝業務員可編輯 qty；
 * FROZEN＝僅組長可調 confirmedQty；
 * CONFIRMED＝皆不可編輯，等待庫務彙總。
 * <p>唯一鍵：(branchCode, purchaseDate)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "branch_purchase_frozen", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_code", "purchase_date"}))
public class BranchPurchaseFrozen extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(name = "branch_code", length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FrozenStatus status;

    @Column(nullable = false)
    private LocalDateTime frozenAt;

    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String frozenBy;

    /** 進入 CONFIRMED 階段才填寫；status 仍為 FROZEN 時為 null（故無 NOT NULL 約束）。 */
    @Column
    private LocalDateTime confirmedAt;

    /** 同 confirmedAt：僅確認後才有值。 */
    @Size(max = 20)
    @Column(length = 20)
    private String confirmedBy;
}
