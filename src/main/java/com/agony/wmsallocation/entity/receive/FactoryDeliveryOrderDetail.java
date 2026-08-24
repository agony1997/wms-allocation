package com.agony.wmsallocation.entity.receive;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

import java.time.LocalDate;

/**
 * 工廠出貨單明細：一筆 = 一項商品的某批次出貨/收貨記錄，隸屬單頭
 * {@link FactoryDeliveryOrder}（以業務單號 fdoNo 關聯）。
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "factory_delivery_order_detail")
public class FactoryDeliveryOrderDetail extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 所屬出貨單號，對應 {@link FactoryDeliveryOrder#getFdoNo()}（以業務單號關聯，非外鍵 id）。 */
    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    @Setter
    private String fdoNo;

    @NotNull
    @Column(nullable = false)
    @Setter
    private Integer itemNo;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    @Setter
    private String productCode;

    @Nationalized
    @NotNull
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    @Setter
    private String productName;

    @NotNull
    @Size(max = 30)
    @Column(length = 30, nullable = false)
    @Setter
    private String batchNo;

    @NotNull
    @Column(nullable = false)
    @Setter
    private LocalDate expiryDate;

    @NotNull
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    @Setter
    private String unit;

    /** 工廠出貨數量；收貨時與 receivedQty 比對，相符轉 RECEIVED、不符轉 DISCREPANCY。 */
    @NotNull
    @Column(nullable = false)
    @Setter
    private Integer qty;

    /** 庫務輸入的實收數量；收貨確認前為 null，入庫即依此數量增加庫存。 */
    @Column
    @Setter
    private Integer receivedQty;
}
