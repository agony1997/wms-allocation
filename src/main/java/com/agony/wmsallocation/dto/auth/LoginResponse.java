package com.agony.wmsallocation.dto.auth;

import java.util.Map;
import java.util.Set;

/**
 * 登入成功回應：JWT 與最基本的登入者資訊（供前端顯示 / 導頁用，敏感欄位不外露）。
 *
 * <p>{@code branchRoles} 與 token 的 {@code branchRoles} claim 是同一份資料，
 * 三處（token／回應／前端 store）刻意同名。前端據此決定頁面顯示與營業所選單，
 * 但那只是 UX——真正的授權在 {@code JwtInterceptor} 與 Service 層，前端可被繞過。
 *
 * <p>刻意<b>不含</b>單值 {@code role} 或 {@code defaultBranchCode}：一人多角時前者必然是謊話，
 * 而系統無「主要營業所」概念，預設營業所由前端依當前頁面所需角色自行推導。
 * 詳見 {@code docs/requirements/specification/master/User.md}「登入契約與前端營業所選擇」。
 */
public record LoginResponse(
        String token,
        String userCode,
        String userName,
        Map<String, Set<String>> branchRoles) {
}
