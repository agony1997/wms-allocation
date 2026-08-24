package com.agony.wmsallocation.entity.enums;

/**
 * 主檔資料的啟用狀態：INACTIVE 視為軟刪除/停用，業務查詢通常只取 ACTIVE。
 */
public enum ActiveStatus {
    ACTIVE,
    INACTIVE
}
