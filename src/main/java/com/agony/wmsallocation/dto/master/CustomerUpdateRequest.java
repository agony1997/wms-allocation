package com.agony.wmsallocation.dto.master;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新客戶的請求。
 *
 * <p>識別「哪一筆」由 URL 的 {@code customerCode}（@PathVariable）決定，故 body 不含
 * {@code customerCode}（業務主鍵不可被更新）；亦不含 {@code status}（啟用/停用屬另一種操作）。
 */
public record CustomerUpdateRequest(
        @NotBlank @Size(max = 100) String customerName,
        @NotBlank @Size(max = 20) String salesOrgCode,
        @NotBlank @Size(max = 20) String userCode,
        @Size(max = 200) String address,
        @Size(max = 20) String phone) {
}
