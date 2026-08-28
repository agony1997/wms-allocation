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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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

    /** 走到簽發 token 那一步的前置：帳號存在、啟用、密碼正確。 */
    private void givenValidCredentials(AuthUser user) {
        when(authUserRepo.findByUserCode(user.getUserCode())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);
    }

    @Test
    @DisplayName("login 同一營業所有兩個角色 - 兩個都要進 token，不可挑一個")
    void login_whenTwoRolesInSameBranch_shouldKeepBoth() {
        AuthUser user = stubUser("U001", ActiveStatus.ACTIVE);
        givenValidCredentials(user);
        when(authUserBranchRoleRepo.findByUserCode("U001"))
                .thenReturn(List.of(stubRole("U001", "1000", "SALES"), stubRole("U001", "1000", "LEADER")));
        when(jwtUtil.generateToken(eq("U001"), any())).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("U001", "password123"));

        // 權限矩陣裡「建立 SPO」SALES 可做、「凍結」只有 LEADER 可做，
        // 挑一個就必然失去另一邊的功能——這是整個多角色改造的理由
        assertThat(response.branchRoles()).containsOnlyKeys("1000");
        assertThat(response.branchRoles().get("1000")).containsExactlyInAnyOrder("SALES", "LEADER");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userCode()).isEqualTo("U001");
        assertThat(response.userName()).isEqualTo("測試員");
    }

    @Test
    @DisplayName("login 跨營業所不同角色 - 應依營業所分組")
    void login_whenRolesAcrossBranches_shouldGroupByBranch() {
        AuthUser user = stubUser("U001", ActiveStatus.ACTIVE);
        givenValidCredentials(user);
        when(authUserBranchRoleRepo.findByUserCode("U001")).thenReturn(List.of(
                stubRole("U001", "1000", "LEADER"),
                stubRole("U001", "1000", "SALES"),
                stubRole("U001", "2000", "SALES")));
        when(jwtUtil.generateToken(eq("U001"), any())).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("U001", "password123"));

        assertThat(response.branchRoles()).containsOnlyKeys("1000", "2000");
        assertThat(response.branchRoles().get("1000")).containsExactlyInAnyOrder("LEADER", "SALES");
        assertThat(response.branchRoles().get("2000")).containsExactly("SALES");
    }

    @Test
    @DisplayName("login 簽進 token 的與回應的是同一份 branchRoles")
    void login_shouldSignSameBranchRolesAsReturned() {
        AuthUser user = stubUser("U001", ActiveStatus.ACTIVE);
        givenValidCredentials(user);
        when(authUserBranchRoleRepo.findByUserCode("U001"))
                .thenReturn(List.of(stubRole("U001", "1100", "WAREHOUSE")));
        when(jwtUtil.generateToken(eq("U001"), any())).thenReturn("jwt-token");

        LoginResponse response = authService.login(new LoginRequest("U001", "password123"));

        // 前端拿回應決定畫面、後端拿 token 決定授權，兩者不同步的話畫面會與實際權限脫節
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Set<String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(jwtUtil).generateToken(eq("U001"), captor.capture());
        assertThat(captor.getValue()).isEqualTo(response.branchRoles());
    }

    @Test
    @DisplayName("login 無任何角色關聯 - 應拋 AUTH_NO_ROLE_ASSIGNED 且不簽發 token")
    void login_whenNoRoleAssigned_shouldThrowAndNotIssueToken() {
        AuthUser user = stubUser("U001", ActiveStatus.ACTIVE);
        givenValidCredentials(user);
        when(authUserBranchRoleRepo.findByUserCode("U001")).thenReturn(List.of());

        // 放行的話他拿得到 token 卻每支端點都 403、營業所選單為空，是登入後無事可做的死路
        assertThatThrownBy(() -> authService.login(new LoginRequest("U001", "password123")))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AUTH_NO_ROLE_ASSIGNED));
        verify(jwtUtil, never()).generateToken(any(), any());
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
