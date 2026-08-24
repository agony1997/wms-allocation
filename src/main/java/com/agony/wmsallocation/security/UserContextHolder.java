package com.agony.wmsallocation.security;

// 用 ThreadLocal 保存「當前請求的登入者資訊」，相當於 Spring Security 的 SecurityContextHolder。
// ThreadLocal 是「每條執行緒各有一份」的變數：Servlet 容器一個請求配一條執行緒，
// 所以在同一請求內任何地方 get 到的，都是這條請求驗過的那位使用者，執行緒之間互不干擾。
public class UserContextHolder {
    // static final：整個 JVM 共用這兩個 ThreadLocal 容器，但容器內的值是各執行緒獨立的
    private static final ThreadLocal<String> userCodeHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> roleHolder = new ThreadLocal<>();

    // 由 JwtInterceptor 在驗證通過後寫入
    public static void setUserCode(String userCode) {
        userCodeHolder.set(userCode);
    }

    // 由 Service 層讀取（例如寫 audit 欄位、或做下轄資料判斷時取當前使用者）
    public static String getUserCode() {
        return userCodeHolder.get();
    }

    public static void setRole(String role) {
        roleHolder.set(role);
    }

    public static String getRole() {
        return roleHolder.get();
    }

    // 請求結束時由 JwtInterceptor.afterCompletion 呼叫。
    // remove() 而非 set(null)：徹底移除才能避免執行緒重用時殘留舊值，也避免記憶體洩漏。
    public static void clear() {
        userCodeHolder.remove();
        roleHolder.remove();
    }
}
