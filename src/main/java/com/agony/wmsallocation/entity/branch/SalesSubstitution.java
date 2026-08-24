package com.agony.wmsallocation.entity.branch;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * 業務員代理人設定：某業務員缺席時，由代理人於指定日期代為處理其業務。
 *
 * <p>唯一鍵：(absentUserCode, substituteDate)——同一缺席者同一天只能設定一位代理人。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "sales_substitution",
        uniqueConstraints = @UniqueConstraint(columnNames = {"absent_user_code", "substitute_date"}))
public class SalesSubstitution extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** 缺席（被代理）的業務員代碼。 */
    @NotNull
    @Size(max = 20)
    @Column(name = "absent_user_code", length = 20, nullable = false)
    private String absentUserCode;

    /** 代理人（接手業務）的業務員代碼。 */
    @NotNull
    @Size(max = 20)
    @Column(name = "substitute_user_code", length = 20, nullable = false)
    private String substituteUserCode;

    @NotNull
    @Column(name = "substitute_date", nullable = false)
    private LocalDate substituteDate;

    /** 代理行為發生所在的營業所：代理具區域性，用以界定代理範圍。 */
    @NotNull
    @Size(max = 20)
    @Column(name = "branch_code", length = 20, nullable = false)
    private String branchCode;
}
