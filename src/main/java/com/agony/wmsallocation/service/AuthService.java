package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.auth.LoginRequest;
import com.agony.wmsallocation.dto.auth.LoginResponse;
import com.agony.wmsallocation.entity.auth.AuthUser;
import com.agony.wmsallocation.entity.auth.AuthUserBranchRole;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.repository.AuthUserBranchRoleRepo;
import com.agony.wmsallocation.repository.AuthUserRepo;
import com.agony.wmsallocation.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 登入認證：驗證帳密、解析角色、簽發 JWT。
 */
@RequiredArgsConstructor
@Service
public class AuthService {

    private final AuthUserRepo authUserRepo;
    private final AuthUserBranchRoleRepo authUserBranchRoleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // 查無帳號 / 密碼錯誤 / 帳號停用，一律回相同訊息，避免洩漏「帳號是否存在」。
        // ponytail: 未做等時比對（查無帳號會早退、略過 bcrypt），mock 學習專案可接受；要防時序側錄再補
        AuthUser user = authUserRepo.findByUserCode(request.userCode())
                .orElseThrow(AuthService::badCredentials);

        if (user.getStatus() != ActiveStatus.ACTIVE) {
            throw badCredentials();
        }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw badCredentials();
        }

        Map<String, Set<String>> branchRoles = resolveBranchRoles(user);
        String token = jwtUtil.generateToken(user.getUserCode(), branchRoles);

        return new LoginResponse(token, user.getUserCode(), user.getUserName(), branchRoles);
    }

    /**
     * 解析使用者在各營業所的角色，形狀為 {@code {branchCode: [roleCode...]}}，
     * 與 token 的 {@code branchRoles} claim 一致。
     *
     * <p>一人可在同一營業所兼多個角色，故不「挑一個」——挑了就無法同時滿足
     * 權限矩陣中 SALES 與 LEADER 各自的功能。
     *
     * <p>一筆角色關聯都沒有的帳號在此擋下：放行的話他拿得到 token，但每支
     * {@code @RequireRole} 端點都會回 403、前端營業所選單是空的，是登入後無事可做的死路。
     * 詳見 {@code docs/requirements/specification/master/User.md}「無角色關聯的使用者不得登入」。
     */
    private Map<String, Set<String>> resolveBranchRoles(AuthUser user) {
        List<AuthUserBranchRole> roles = authUserBranchRoleRepo.findByUserCode(user.getUserCode());
        if (roles.isEmpty()) {
            throw new BusinessRuleException("帳號未指派任何營業所角色，請洽系統管理員",
                    ErrorCode.AUTH_NO_ROLE_ASSIGNED);
        }
        return roles.stream()
                .collect(Collectors.groupingBy(
                        AuthUserBranchRole::getBranchCode,
                        Collectors.mapping(AuthUserBranchRole::getRoleCode, Collectors.toSet())));
    }

    private static BusinessRuleException badCredentials() {
        return new BusinessRuleException("帳號或密碼錯誤", ErrorCode.AUTH_BAD_CREDENTIALS);
    }
}
