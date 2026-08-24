package com.agony.wmsallocation.entity.master;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.Nationalized;

import java.math.BigDecimal;

/**
 * 商品主檔：訂貨、配貨、庫存等流程共用的商品基本資料。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/Product.md}。
 */
@Data
@Entity
@Table(name = "product")
public class Product extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(unique = true, length = 20, nullable = false)
    private String productCode;

    @Nationalized
    @NotNull
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String productName;

    /** 庫存與單價計價的基準單位，亦為 ProductUnitConversion 換算的基準。 */
    @NotNull
    @Size(max = 10)
    @Column(length = 10, nullable = false)
    private String baseUnit;

    @NotNull
    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ActiveStatus status;
}
