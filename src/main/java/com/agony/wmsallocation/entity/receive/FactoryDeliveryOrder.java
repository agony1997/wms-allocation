package com.agony.wmsallocation.entity.receive;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.receive.enums.FactoryDeliveryStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工廠出貨單（FDO）：工廠依營業所訂貨單（BPO）出貨後產生的單據（Mock 中由工廠模擬頁面手動觸發產生）；
 * 庫務核對批次/效期/數量並確認收貨後，依「實收數量」觸發庫存（RECEIVE）入庫。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/receive/FactoryDeliveryOrder.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "factory_delivery_order")
public class FactoryDeliveryOrder extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 30)
    @Column(unique = true, length = 30, nullable = false)
    private String fdoNo;

    /** 來源營業所訂貨單號（指向上游 BPO）；一張 BPO 僅能出貨一次。 */
    @NotNull
    @Size(max = 30)
    @Column(unique = true, length = 30, nullable = false)
    private String bpoNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String factoryCode;

    @NotNull
    @Column(nullable = false)
    private LocalDate deliveryDate;

    /** 收貨狀態流轉：PENDING（待收貨）→ 收貨後依數量是否相符轉 RECEIVED 或 DISCREPANCY。 */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private FactoryDeliveryStatus status;

    /** 收貨確認時間；未收貨前為 null。 */
    private LocalDateTime receivedAt;

    /** 收貨人員工編；未收貨前為 null。 */
    @Size(max = 20)
    @Column(length = 20)
    private String receivedBy;

    @Nationalized
    @Size(max = 200)
    @Column(length = 200)
    private String remark;
}
