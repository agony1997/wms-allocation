package com.agony.wmsallocation.entity.allocation;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 配貨單 (AO)：庫務依 SPOD 從大庫 FIFO 扣庫、分配商品給各業務員的單頭。
 *
 * <p>粒度為「營業所 + 日期」，同一營業所同日多次配貨會產生多張 AO。
 * 業務規則詳見 {@code docs/requirements/specification/allocation/AllocationOrder.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "allocation_order")
public class AllocationOrder extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(unique = true, length = 30, nullable = false)
    private String allocationNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Column(nullable = false)
    private LocalDate allocationDate;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AllocationStatus status;
}
