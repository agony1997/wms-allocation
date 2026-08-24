package com.agony.wmsallocation.entity.master;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * 商品單位換算表：定義單一商品在不同單位間的換算率。
 *
 * <p>換算為單向：(箱→個) 與 (個→箱) 各存一列、互為倒數，
 * 例如箱→個=12、個→箱=0.0833。業務規則詳見
 * {@code docs/requirements/specification/master/Product.md}。
 * <p>唯一鍵：(productCode, fromUnit, toUnit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "product_unit_conversion", uniqueConstraints = @UniqueConstraint(columnNames = {"product_code", "from_unit", "to_unit"}))
public class ProductUnitConversion extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String productCode;

    @NotNull
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    private String fromUnit;

    @NotNull
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    private String toUnit;

    /** 1 個 fromUnit 等於多少個 toUnit（換算方向為 fromUnit → toUnit）。 */
    @NotNull
    @Column(precision = 10, scale = 4, nullable = false)
    private BigDecimal conversionRate;
}
