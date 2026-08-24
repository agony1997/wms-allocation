package com.agony.wmsallocation.entity.master;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 商品與工廠的多對多對應表：一個商品可來自多個工廠。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/Product.md}。
 * <p>唯一鍵：(productCode, factoryCode)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "product_factory", uniqueConstraints = @UniqueConstraint(columnNames = {"product_code", "factory_code"}))
public class ProductFactory extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String productCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String factoryCode;

    /** 是否為該商品的預設來源工廠；同一商品應只有一筆為 true。 */
    @NotNull
    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean isDefault;
}
