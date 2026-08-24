package com.agony.wmsallocation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 所有 Entity 的稽核基底類別：提供 createdAt / updatedAt / createdBy / updatedBy 四個欄位，
 * 由 JPA Auditing（{@code AuditingEntityListener}）於 persist / update 時自動填入，不需手動設值。
 *
 * <p>auditor 為當前登入使用者；無登入情境（排程、測試）則為 {@code SYSTEM}，見 {@code JpaAuditingConfig}。
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 20, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 20)
    private String updatedBy;
}
