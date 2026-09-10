package com.agony.wmsallocation.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Spring MVC 的攔截器：在請求進到 Controller「之前」先驗身份與權限。
 * 相當於 Spring Security 的 Filter Chain，但攔的是 DispatcherServlet 之後、Handler 之前。
 * 攔截範圍由 WebMvcConfig 註冊。
 */
@SuppressWarnings("NullableProblems")
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果請求的不是 Controller 的方法 (例如靜態資源)，直接放行
        // HandlerMethod 代表「某個 Controller 的某個方法」；不是的話就沒有 @RequireRole 可檢查
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 1. 從 Header 拿出 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 沒帶或格式不對 → 401 未驗證
            return false;
        }

        String token = authHeader.substring(7); // 去掉開頭的 "Bearer " 共 7 個字元，留下純 token

        // 2. 驗證 Token 並取得資料
        Claims claims = jwtUtil.validateAndGetClaims(token);
        if (claims == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 驗證失敗（竄改/過期）→ 401
            return false;
        }

        // 從 Payload 取出當初 generateToken 放進去的身份資訊。
        String userCode = claims.getSubject();          // 對應 subject(userCode)
        Map<String, Set<String>> branchRoles = jwtUtil.extractBranchRoles(claims);

        // 3. 檢查是否有 @RequireRole 權限限制
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null && !hasAnyRoleInAnyBranch(branchRoles, requireRole.value())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 角色不符，403
            return false;
        }

        // 4. 全部通過才把使用者資訊放入 Context
        UserContextHolder.setUserCode(userCode);
        UserContextHolder.setBranchRoles(branchRoles);

        return true;
    }

    private boolean hasAnyRoleInAnyBranch(Map<String, Set<String>> branchRoles, String[] needed) {
        return branchRoles.values()
                .stream()
                .anyMatch(rolesOfBranch -> Arrays.stream(needed).anyMatch(rolesOfBranch::contains));
    }

    /**
     * preHandle 回 false 時，本攔截器的 afterCompletion 不會被呼叫
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
