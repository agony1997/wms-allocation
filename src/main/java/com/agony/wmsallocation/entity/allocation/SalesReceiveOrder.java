package com.agony.wmsallocation.entity.allocation;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 業務領貨單 (SRO)：業務員領取已配貨明細 (AOD) 至自己儲位的單頭。
 *
 * <p>一張 SRO 可彙整來自多張 AO 的明細（AO : SRO = N : 1）。
 * 業務規則詳見 {@code docs/requirements/specification/allocation/SalesReceiveOrder.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_receive_order")
public class SalesReceiveOrder extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(unique = true, length = 30, nullable = false)
    private String receiveNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    /** 領貨的業務員儲位 (CAR)，領取後庫存移轉至此。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @NotNull
    @Column(nullable = false)
    private LocalDate receiveDate;
}
