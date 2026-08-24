package com.agony.wmsallocation.entity.auth;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 角色主檔（SALES / LEADER / WAREHOUSE / ADMIN 等），決定功能權限。
 *
 * <p>角色定義與權限矩陣詳見 {@code docs/requirements/specification/master/User.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "auth_role")
public class AuthRole extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(unique = true, length = 20, nullable = false)
    private String roleCode;

    @Nationalized
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String roleName;
}
