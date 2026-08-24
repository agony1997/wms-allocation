package com.agony.wmsallocation.entity.auth;

import com.agony.wmsallocation.entity.AuditEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 使用者-營業所-角色關聯：同一使用者在不同營業所可擔任不同角色，亦可在同一營業所兼多個角色。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/User.md}。
 * <p>唯一鍵：(userCode, branchCode, roleCode)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "auth_user_branch_role", uniqueConstraints = @UniqueConstraint(columnNames = {"user_code", "branch_code", "role_code"}))
public class AuthUserBranchRole extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String userCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String branchCode;

    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String roleCode;
}
