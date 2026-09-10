package com.agony.wmsallocation.security;

import com.agony.wmsallocation.entity.branch.Location;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.exception.ResourceNotFoundException;
import com.agony.wmsallocation.repository.LocationRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;

/**
 * Service 層的資料範圍授權入口，補 {@code @RequireRole} 沒做的「這筆資料動不動得了」檢查。
 * ADMIN 一律放行，兩個方法都不比對——全系統唯一的角色特例，見 User.md「資料範圍授權」。
 */
@Component
@RequiredArgsConstructor
public class DataScopeGuard {

    private final LocationRepo locationRepo;

    /** 用於 LEADER／WAREHOUSE 的營業所級操作：branchCode 須在 token 的 branchRoles 內，且該所底下要有 requiredRoles 之一。 */
    public void assertBranchAccess(String branchCode, String... requiredRoles) {
        if (UserContextHolder.hasRoleInAnyBranch("ADMIN")) {
            return;
        }
        boolean hasRequiredRole = Arrays.stream(requiredRoles)
                .anyMatch(role -> UserContextHolder.hasRole(branchCode, role));
        if (!hasRequiredRole) {
            throw new BusinessRuleException(
                    "無此營業所的操作權限：branchCode=" + branchCode, ErrorCode.BRANCH_ACCESS_DENIED);
        }
    }

    /** 用於 SALES 的儲位級操作：locationCode 的 Location.userCode 須為目前登入者本人。 */
    public void assertLocationOwnership(String locationCode) {
        if (UserContextHolder.hasRoleInAnyBranch("ADMIN")) {
            return;
        }
        String ownerUserCode = locationRepo.findByLocationCode(locationCode)
                .map(Location::getUserCode)
                .orElseThrow(() -> new ResourceNotFoundException("找不到儲位：locationCode=" + locationCode));
        if (!Objects.equals(ownerUserCode, UserContextHolder.getUserCode())) {
            throw new BusinessRuleException(
                    "無此儲位的操作權限：locationCode=" + locationCode, ErrorCode.LOCATION_ACCESS_DENIED);
        }
    }
}
