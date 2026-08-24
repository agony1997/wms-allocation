package com.agony.wmsallocation.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 建立商品的請求。
 *
 * <p>建立時資源尚不存在，URL（{@code POST /api/products}）指向集合而非單一資源，
 * 故業務主鍵 {@code productCode} 由 body 帶入。{@code status} 不由前端指定，
 * 一律由 Service 預設為 {@code ACTIVE}。
 */
public record ProductCreateRequest(
        @NotBlank @Size(max = 20) String productCode,
        @NotBlank @Size(max = 100) String productName,
        @NotBlank @Size(max = 10) String baseUnit,
        @NotNull BigDecimal basePrice) {
}
