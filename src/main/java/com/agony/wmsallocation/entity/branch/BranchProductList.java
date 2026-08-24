package com.agony.wmsallocation.entity.branch;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 營業所常用產品排序清單：建立新訂單時依此清單帶入預設產品與排序。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/branch/BranchProductList.md}。
 * <p>唯一鍵：(branchCode, productCode, unit)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "branch_product_list", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_code", "product_code", "unit"}))
public class BranchProductList extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String productCode;

    @Nationalized
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String productName;

    @NotNull
    @Size(max = 5)
    @Column(length = 5, nullable = false)
    private String unit;

    @NotNull
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int sortOrder;
}
