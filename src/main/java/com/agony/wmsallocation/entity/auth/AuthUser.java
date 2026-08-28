package com.agony.wmsallocation.entity.auth;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 系統使用者（業務員、組長、庫務、系統管理員等）。
 *
 * <p>本表<b>不掛營業所欄位</b>，系統無「主要營業所」概念：角色歸屬見 {@link AuthUserBranchRole}，
 * 儲位歸屬見 {@code Location}（自帶 branchCode 與 userCode）。兩者是各自獨立的事實——
 * 在某所有角色卻無儲位、或反之，都是合法狀態。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/User.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "auth_user")
public class AuthUser extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(name = "user_code", unique = true, length = 20, nullable = false)
    private String userCode;

    @NotNull
    @Size(max = 50)
    @Column(unique = true, length = 50, nullable = false)
    private String email;

    @Nationalized
    @NotNull
    @Size(max = 15)
    @Column(name = "user_name", nullable = false, length = 15)
    private String userName;

    /** 儲存加密後的密碼雜湊（長度 72 對應 bcrypt hash），非明文。 */
    @NotNull
    @Size(max = 72)
    @Column(nullable = false, length = 72)
    private String password;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ActiveStatus status;
}
