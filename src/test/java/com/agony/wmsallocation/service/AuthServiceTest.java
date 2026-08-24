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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepo authUserRepo;
    @Mock
    private AuthUserBranchRoleRepo authUserBranchRoleRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private AuthService authService;

    private AuthUser stubUser(String userCode, ActiveStatus status) {
        AuthUser user = new AuthUser();
        user.setUserCode(userCode);
        user.setUserName("測試員");
        user.setPassword("$2a$10$hash");
        user.setBranchCode("BR01");
        user.setStatus(status);
        return user;
    }

    private AuthUserBranchRole stubRole(String userCode, String branchCode, String roleCode) {
        AuthUserBranchRole role = new AuthUserBranchRole();
        role.setUserCode(userCode);
        role.setBranchCode(branchCode);
        role.setRoleCode(roleCode);
        return role;
    }

    @Test
    @DisplayName("login 帳密正確 - 應回傳 token 與主要營業所的角色")
    void login_whenValid_shouldReturnTokenAndPrimaryBranchRole() {
        AuthUser user = stubUser("U001", ActiveStatus.ACTIVE);
        when(authUserRepo.findByUserCode("U001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);
        // 故意讓非主要營業所(BR02)的角色排在前面，驗證解析會優先挑主要營業所 BR01
        when(authUserBranchRoleRepo.findByUserCode("U001"))
                .thenReturn(List.of(stubRole("U001", "BR02", "LEADER"), stubRole("U001", "BR01", "SALES")));
        when(jwtUtil.generateToken("U001", "SALES")).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("U001", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userCode()).isEqualTo("U001");
        assertThat(response.role()).isEqualTo("SALES");        // 取主要營業所 BR01，而非清單第一筆 BR02
        assertThat(response.branchCode()).isEqualTo("BR01");
    }

    @Test
    @DisplayName("login 密碼錯誤 - 應拋 AUTH_BAD_CREDENTIALS 且不簽發 token")
    void login_whenWrongPassword_shouldThrowAndNotIssueToken() {
        AuthUser user = stubUser("U001", ActiveStatus.ACTIVE);
        when(authUserRepo.findByUserCode("U001")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("U001", "wrong")))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS));
        verify(jwtUtil, never()).generateToken(any(), any());
    }

    @Test
    @DisplayName("login 查無帳號 - 應拋 AUTH_BAD_CREDENTIALS")
    void login_whenUserNotFound_shouldThrow() {
        when(authUserRepo.findByUserCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("NOPE", "password123")))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS));
    }

    @Test
    @DisplayName("login 帳號停用 - 應拋 AUTH_BAD_CREDENTIALS 且不驗密碼")
    void login_whenInactive_shouldThrowBeforeCheckingPassword() {
        AuthUser user = stubUser("U001", ActiveStatus.INACTIVE);
        when(authUserRepo.findByUserCode("U001")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("U001", "password123")))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_BAD_CREDENTIALS));
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
