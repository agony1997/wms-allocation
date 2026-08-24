package com.agony.wmsallocation.entity.branch;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 業務員配貨優先度：配貨時決定批次分配順序，優先度高者先分到效期較長的批次。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/branch/SalesPriority.md}。
 * <p>唯一鍵：(branchCode, locationCode)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_priority", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_code", "location_code"}))
public class SalesPriority extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    /** 此處指業務員車存（CAR）儲位代碼。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    /** 優先等級，數字越小越優先（1 = 最高）。 */
    @NotNull
    @Column(nullable = false)
    private Integer priorityLevel;
}
