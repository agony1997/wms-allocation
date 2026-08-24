package com.agony.wmsallocation.config;

import com.agony.wmsallocation.security.UserContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA Auditing 配置。
 * auditor 優先取當前登入者（由 JwtInterceptor 寫入 UserContextHolder）；
 * 無登入情境（排程、測試）時回退為 SYSTEM。
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.ofNullable(UserContextHolder.getUserCode()).or(() -> Optional.of("SYSTEM"));
    }
}
