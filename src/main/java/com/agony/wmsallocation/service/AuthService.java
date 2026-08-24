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

        String role = resolveRole(user);
        String token = jwtUtil.generateToken(user.getUserCode(), role);
        return new LoginResponse(token, user.getUserCode(), user.getUserName(), role, user.getBranchCode());
    }

    // JWT 目前只裝單一角色，故從使用者的角色關聯挑一個：優先取其主要營業所的角色，取不到再退取任一筆。
    // ponytail: 一人多角只會取到一個；要支援多角色需改 token 結構 + @RequireRole + 攔截器三處
    private String resolveRole(AuthUser user) {
        List<AuthUserBranchRole> roles = authUserBranchRoleRepo.findByUserCode(user.getUserCode());
        return roles.stream()
                .filter(r -> r.getBranchCode().equals(user.getBranchCode()))
                .map(AuthUserBranchRole::getRoleCode)
                .findFirst()
                .orElseGet(() -> roles.stream()
                        .map(AuthUserBranchRole::getRoleCode)
                        .findFirst()
                        .orElse(null));
    }

    private static BusinessRuleException badCredentials() {
        return new BusinessRuleException("帳號或密碼錯誤", ErrorCode.AUTH_BAD_CREDENTIALS);
    }
}
