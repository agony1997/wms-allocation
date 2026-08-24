package com.agony.wmsallocation.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 更新商品的請求。
 *
 * <p>識別「哪一筆」由 URL 的 {@code productCode}（@PathVariable）決定，故 body 不含
 * {@code productCode}（業務主鍵不可被更新）；亦不含 {@code status}（啟用/停用屬另一種操作）。
 */
public record ProductUpdateRequest(
        @NotBlank @Size(max = 100) String productName,
        @NotBlank @Size(max = 10) String baseUnit,
        @NotNull BigDecimal basePrice) {
}
