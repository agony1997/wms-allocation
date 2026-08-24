package com.agony.wmsallocation.entity.master;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 客戶主檔：預訂、送貨、應收帳款等流程的客戶基本資料。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/Customer.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "customer")
public class Customer extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(unique = true, length = 20, nullable = false)
    private String customerCode;

    @Nationalized
    @NotNull
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String customerName;

    /** 客戶所屬的銷售組織，指向 SalesOrganization.salesOrgCode。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String salesOrgCode;

    /** 負責此客戶的業務員，指向 AuthUser.userCode。 */
    @NotNull
    @Size(max = 20)
    @Column(length = 20, nullable = false)
    private String userCode;

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
