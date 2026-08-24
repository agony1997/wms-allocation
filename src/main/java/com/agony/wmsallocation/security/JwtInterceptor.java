package com.agony.wmsallocation.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

// Spring MVC 的攔截器：在請求進到 Controller「之前」先驗身份與權限。
// 相當於 Spring Security 的 Filter Chain，但攔的是 DispatcherServlet 之後、Handler 之前。
// 攔截範圍由 WebMvcConfig 註冊（本專案只攔 /api/**）。
@SuppressWarnings("NullableProblems")
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    // preHandle 回傳 true 才會進到 Controller；回傳 false 代表這裡就攔下、不往下走。
    // handler 是即將處理此請求的目標（可能是 Controller 方法，也可能是靜態資源處理器）。
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 如果請求的不是 Controller 的方法 (例如靜態資源)，直接放行
        // HandlerMethod 代表「某個 Controller 的某個方法」；不是的話就沒有 @RequireRole 可檢查
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // 1. 從 Header 拿出 Token
        // HTTP 慣例：Authorization: Bearer <token>
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

        // 從 Payload 取出當初 generateToken 放進去的身份資訊
        String userCode = claims.getSubject();          // 對應 subject(userCode)
        String role = claims.get("role", String.class); // 對應 claim("role", role)

        // 3. 檢查是否有 @RequireRole 權限限制
        // 注意：getMethodAnnotation 只讀「方法上」的標註，標在 Controller 類別上的不會被讀到
        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole != null) {
            String neededRole = requireRole.value();     // 這支 API 要求的角色
            if (!neededRole.equals(role)) {              // 精確比對：角色不完全相同就擋
                response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 角色不符，403
                return false;
            }
        }

        // 4. 全部通過才把使用者資訊放入 Context
        // 存進 ThreadLocal，讓同一條請求執行緒中的 Service 層可直接取用，不必層層傳參數。
        // 為何等到這裡才寫：preHandle 回 false 時，本攔截器的 afterCompletion 不會被呼叫（見下方註解），
        // 若在角色檢查前就寫，403 這條路的 clear() 不會執行，值會殘留到下一個重用此執行緒的請求。
        UserContextHolder.setUserCode(userCode);
        UserContextHolder.setRole(role);

        return true; // 身份與權限都通過，放行進 Controller
    }

    // 請求處理完畢後呼叫（即使中途拋例外也會，方便清理資源）。
    // 但前提是「本攔截器的 preHandle 已成功回傳 true」；preHandle 回 false 時這裡不會被呼叫，
    // 所以 preHandle 中要在確定放行後才佔用需要清理的資源（見上面 setUserCode 的時機）。
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 請求結束後，務必清除 ThreadLocal 避免記憶體洩漏與資料污染
        // Tomcat 執行緒會被重用，不清的話下一個請求可能讀到上一位使用者的身份
        UserContextHolder.clear();
    }
}
