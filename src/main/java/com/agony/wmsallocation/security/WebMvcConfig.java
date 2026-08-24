package com.agony.wmsallocation.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Spring MVC 設定：把 JwtInterceptor 掛上去並指定它要攔哪些路徑。
// 實作 WebMvcConfigurer 可在不覆蓋 Spring 預設設定的前提下，追加自己的攔截器。
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    // 注入上面定義的攔截器 bean
    private final JwtInterceptor jwtInterceptor;

    // Spring 啟動時呼叫，讓我們把攔截器註冊進攔截鏈並設定生效範圍
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")         // 攔截所有 /api 開頭的請求
                .excludePathPatterns("/api/auth/login"); // 登入 API 不需要驗證 Token
                // 排除登入是因為：還沒登入就不可能有 token，若不排除會變成「要 token 才能登入」的死結
    }
}
