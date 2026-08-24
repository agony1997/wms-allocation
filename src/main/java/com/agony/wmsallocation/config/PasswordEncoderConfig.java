package com.agony.wmsallocation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 提供密碼雜湊器。用 spring-security-crypto 的 BCrypt，不引入整套 Spring Security
 * （本專案認證是手刻 JWT + 攔截器，只需要「驗 bcrypt 雜湊」這一件事）。
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
