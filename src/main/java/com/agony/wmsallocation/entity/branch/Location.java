package com.agony.wmsallocation.entity.branch;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 儲位主檔：庫存存放位置，分大庫（WAREHOUSE）與業務員車存（CAR）兩類。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/Location.md}。
 * <p>唯一鍵：(branchCode, locationCode)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "location", uniqueConstraints = @UniqueConstraint(columnNames = {"branch_code", "location_code"}))
public class Location extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Integer id;

    /** WAREHOUSE 時 = branchCode；CAR 時為業務員儲位代碼（如 S001）。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String locationCode;

    @Nationalized
    @NotNull
    @Size(max = 40)
    @Column(length = 40, nullable = false)
    private String locationName;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    /** 所屬業務員：僅 CAR 有值，WAREHOUSE 為 null。 */
    @Size(max = 20)
    @Column(length = 20)
    private String userCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private LocationType locationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ActiveStatus status;
}
