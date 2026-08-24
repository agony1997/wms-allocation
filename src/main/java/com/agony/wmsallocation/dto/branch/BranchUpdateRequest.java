package com.agony.wmsallocation.dto.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新營業所的請求。
 *
 * <p>識別「哪一筆」由 URL 的 {@code branchCode}（@PathVariable）決定，故 body 不含
 * {@code branchCode}（業務主鍵不可被更新）；亦不含 {@code status}（啟用/停用屬另一種操作）。
 */
public record BranchUpdateRequest(
        @NotBlank @Size(max = 20) String salesOrgCode,
        @NotBlank @Size(max = 40) String branchName,
        @Size(max = 200) String address,
        @Size(max = 20) String phone) {
}
