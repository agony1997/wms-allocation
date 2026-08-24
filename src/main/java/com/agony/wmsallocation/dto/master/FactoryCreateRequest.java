package com.agony.wmsallocation.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 建立工廠的請求。
 *
 * <p>建立時資源尚不存在，URL（{@code POST /api/factories}）指向集合而非單一資源，
 * 故業務主鍵 {@code factoryCode} 由 body 帶入。{@code status} 不由前端指定，
 * 一律由 Service 預設為 {@code ACTIVE}。
 */
public record FactoryCreateRequest(
        @NotBlank @Size(max = 20) String factoryCode,
        @NotBlank @Size(max = 100) String factoryName,
        @Size(max = 200) String address,
        @Size(max = 20) String phone) {
}
