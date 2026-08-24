package com.agony.wmsallocation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 排程配置。Spring Boot 不會自動啟用 @Scheduled，缺這個 annotation 的話
 * InventoryService 的每日庫存快照 cron 不會觸發（只剩手動 API 可用）。
 *
 * <p>單機單副本前提；多副本部署時每個 instance 都會各跑一次，屆時需外移排程
 * （見 README 階段 5）。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
