package com.agony.wmsallocation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 提供可注入的 {@link Clock}，讓依賴「今天」的業務邏輯（如訂貨日 D+2~D+9 區間）
 * 能在測試以 {@code Clock.fixed(...)} 固定時間、可重現。
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
