package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.auth.LoginRequest;
import com.agony.wmsallocation.dto.auth.LoginResponse;
import com.agony.wmsallocation.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認證端點。登入路徑（/api/auth/login）已在 WebMvcConfig 排除 JWT 攔截，
 * 否則會變成「要 token 才能登入」的死結。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
