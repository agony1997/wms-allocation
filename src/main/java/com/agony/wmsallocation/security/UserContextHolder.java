package com.agony.wmsallocation.security;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 用 ThreadLocal 保存「當前請求的登入者資訊」，相當於 Spring Security 的 SecurityContextHolder。
// ThreadLocal 是「每條執行緒各有一份」的變數：Servlet 容器一個請求配一條執行緒，
// 所以在同一請求內任何地方 get 到的，都是這條請求驗過的那位使用者，執行緒之間互不干擾。
//
// 持有的是 userCode + branchRoles（{branchCode: [roleCode...]}），與 token 的 claim 同一份資料，
// 供 @RequireRole（功能授權）與 Service 層（資料範圍授權）共用同一個來源。
public class UserContextHolder {
    // static final：整個 JVM 共用這兩個 ThreadLocal 容器，但容器內的值是各執行緒獨立的
    private static final ThreadLocal<String> userCodeHolder = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Set<String>>> branchRolesHolder = new ThreadLocal<>();

    // 由 JwtInterceptor 在驗證通過後寫入
    public static void setUserCode(String userCode) {
        userCodeHolder.set(userCode);
    }

    // 由 Service 層讀取（例如寫 audit 欄位、或做下轄資料判斷時取當前使用者）
    public static String getUserCode() {
        return userCodeHolder.get();
    }

    // 存入時做深層不可變複製：Map.copyOf 只凍結外層，內層 Set 仍可被改。
    // 原本存 String 天生不可變所以沒這個問題；換成 map 之後若不複製，
    // Service 層任何一處都能往自己的權限裡塞角色——那等於把授權交給被授權者自己決定。
    public static void setBranchRoles(Map<String, Set<String>> branchRoles) {
        branchRolesHolder.set(branchRoles == null ? Map.of() : branchRoles.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue()))));
    }

    // 未設定時回空 map 而非 null：呼叫端不必各自防 null，且「沒有權限」本來就是空集合的語意
    public static Map<String, Set<String>> getBranchRoles() {
        Map<String, Set<String>> branchRoles = branchRolesHolder.get();
        return branchRoles == null ? Map.of() : branchRoles;
    }

    // 資料範圍授權用：他在「這個」營業所有沒有這個角色。
    // Service 層判斷「能不能對某營業所的資料動手」時用這支——攔截器的 @RequireRole 不做營業所比對，
    // 只回答「他是不是這個工種」，所以這一層的檢查是必要而非加分（見 User.md「資料範圍授權」）。
    //
    // 注意：ADMIN「不限營業所」是規格層的特例，不在這裡處理。
    // 這支方法只誠實回報 token 裡有什麼，特例由 Service 層的 assertBranchAccess 統一套用，
    // 免得每個呼叫點各自記得要放行 ADMIN。
    public static boolean hasRole(String branchCode, String roleCode) {
        return getBranchRoles().getOrDefault(branchCode, Set.of()).contains(roleCode);
    }

    // 請求結束時由 JwtInterceptor.afterCompletion 呼叫。
    // remove() 而非 set(null)：徹底移除才能避免執行緒重用時殘留舊值，也避免記憶體洩漏。
    // 新增 ThreadLocal 時務必同步加進這裡——漏掉的話 Tomcat 重用執行緒時，
    // 上一個請求的權限會被下一個人讀到，那比記憶體洩漏嚴重得多。
    public static void clear() {
        userCodeHolder.remove();
        branchRolesHolder.remove();
    }
}
