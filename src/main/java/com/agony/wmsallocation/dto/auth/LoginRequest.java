package com.agony.wmsallocation.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 登入請求：以使用者代號 + 密碼認證。
 */
public record LoginRequest(
        @NotBlank @Size(max = 20) String userCode,
        @NotBlank @Size(max = 100) String password) {
}
