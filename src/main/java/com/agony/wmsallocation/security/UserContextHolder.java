package com.agony.wmsallocation.security;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 用 ThreadLocal 保存「當前請求的登入者資訊」，相當於 Spring Security 的 SecurityContextHolder。
 */
public class UserContextHolder {
    private static final ThreadLocal<String> userCodeHolder = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Set<String>>> branchRolesHolder = new ThreadLocal<>();

    public static void setUserCode(String userCode) {
        userCodeHolder.set(userCode);
    }

    public static String getUserCode() {
        return userCodeHolder.get();
    }

    public static void setBranchRoles(Map<String, Set<String>> branchRoles) {
        if (branchRoles == null) {
            branchRolesHolder.set(Map.of());
        } else {
            // 預防 : 存入時做深層不可變複製，斷開記憶體參照
            Map<String, Set<String>> setMap = branchRoles.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> Set.copyOf(e.getValue())));

            branchRolesHolder.set(setMap);
        }
    }

    // 未設定時回空 map 而非 null：呼叫端不必各自防 null，且「沒有權限」本來就是空集合的語意
    public static Map<String, Set<String>> getBranchRoles() {
        Map<String, Set<String>> branchRoles = branchRolesHolder.get();
        return branchRoles == null ? Map.of() : branchRoles;
    }

    public static boolean hasRole(String branchCode, String roleCode) {
        return getBranchRoles().getOrDefault(branchCode, Set.of()).contains(roleCode);
    }

    public static void clear() {
        userCodeHolder.remove();
        branchRolesHolder.remove();
    }
}
