package com.agony.wmsallocation.entity.purchase;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 業務員自訂產品清單：業務員為自己儲位維護的常用品項與預帶數量，供訂貨時快速帶入。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/purchase/SalesPurchaseList.md}。
 * <p>層級為儲位（locationCode），與營業所層級的營業所產品清單 (BranchProductList) 為獨立兩套系統。
 * <p>唯一鍵：(locationCode, productCode, unit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_purchase_list", uniqueConstraints = @UniqueConstraint(columnNames = {"location_code", "product_code", "unit"}))
public class SalesPurchaseList extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @NotNull
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String productCode;

    @NotNull
    @Size(max = 5)
    @Column(nullable = false, length = 5)
    private String unit;

    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int qty;

    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int sortOrder;
}
