package com.agony.wmsallocation.dto.auth;

/**
 * 登入成功回應：JWT 與最基本的登入者資訊（供前端顯示 / 導頁用，敏感欄位不外露）。
 */
public record LoginResponse(
        String token,
        String userCode,
        String userName,
        String role,
        String branchCode) {
}
