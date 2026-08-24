package com.agony.wmsallocation.entity.branch;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 營業所主檔：系統最主要的組織單位，下轄儲位、人員、客戶與各類單據。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/Branch.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "branch")
public class Branch extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(unique = true, length = 20, nullable = false)
    private String branchCode;

    /** 所屬銷售組織代碼，指向上層組織 SalesOrganization。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String salesOrgCode;

    @Nationalized
    @NotNull
    @Size(max = 40)
    @Column(length = 40, nullable = false)
    private String branchName;

    @Nationalized
    @Size(max = 200)
    @Column(length = 200)
    private String address;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ActiveStatus status;
}
