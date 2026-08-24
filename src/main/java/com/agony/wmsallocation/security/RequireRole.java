package com.agony.wmsallocation.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 自訂標註：標在 Controller 的 API 方法上，宣告「呼叫這支 API 需要什麼角色」。
// 由 JwtInterceptor 讀取並執行檢查，角色不符就回 403。相當於 Spring Security 的 @PreAuthorize。

// @Target：允許標註的位置。刻意只開放 METHOD——JwtInterceptor 用 getMethodAnnotation 讀，
// 只看得到方法層級的標註。若開放 TYPE，標在類別上會編譯通過但完全不生效，
// 變成「以為有保護、其實沒有」的安全性陷阱；限制在 METHOD 讓誤用直接編譯失敗。
// 日後要支援類別層級，須同時改攔截器（補讀 handlerMethod.getBeanType() 上的標註）。
@Target(ElementType.METHOD)
// @Retention RUNTIME：標註資訊要保留到執行期，攔截器才能在跑的時候用反射讀到它。
// （預設的 CLASS 只留到位元碼、執行期讀不到）
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    // 標註的值，寫成 @RequireRole("MANAGER")。目前只支援單一角色、精確比對。
    String value(); // 用來標示需要的角色，例如 "MANAGER"
}
