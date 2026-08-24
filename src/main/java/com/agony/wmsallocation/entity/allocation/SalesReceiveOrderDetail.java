package com.agony.wmsallocation.entity.allocation;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 業務領貨單明細 (SROD)：一筆領貨對應一筆來源 AOD（AOD : SROD = 1 : 1，一次領完）。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/allocation/SalesReceiveOrder.md}。
 * <p>唯一鍵：(receiveNo, itemNo)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_receive_order_detail",
        uniqueConstraints = @UniqueConstraint(columnNames = {"receive_no", "item_no"}))
public class SalesReceiveOrderDetail extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String receiveNo;

    @NotNull
    @Column(nullable = false)
    private Integer itemNo;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    private String allocationNo;

    /** 與 allocationNo 共同指向來源 AOD 的 (allocationNo, itemNo)。 */
    @NotNull
    @Column(nullable = false)
    private Integer allocationItemNo;

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

    /** 領取數量；因一次領完，應等於來源 AOD.allocatedQty。 */
    @NotNull
    @Column(nullable = false)
    private Integer qty;
}
