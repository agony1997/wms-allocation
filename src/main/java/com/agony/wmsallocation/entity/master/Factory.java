package com.agony.wmsallocation.entity.master;

import com.agony.wmsallocation.entity.AuditEntity;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Nationalized;

/**
 * 工廠主檔：供商品來源對應、訂貨、銷退等流程引用的工廠基本資料。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/master/Factory.md}。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "factory")
public class Factory extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(max = 20)
    @Column(unique = true, length = 20, nullable = false)
    private String factoryCode;

    @Nationalized
    @NotNull
    @Size(max = 100)
    @Column(length = 100, nullable = false)
    private String factoryName;

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
