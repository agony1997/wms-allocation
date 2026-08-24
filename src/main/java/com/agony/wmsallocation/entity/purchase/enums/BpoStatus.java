package com.agony.wmsallocation.entity.purchase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 營業所訂貨單 (BPO) 狀態。PENDING 為送工廠後的初始狀態；
 * 收貨時依實收與訂購是否相符，轉為 RECEIVED（相符）或 DISCREPANCY（有差異）。
 */
@Getter
@RequiredArgsConstructor
public enum BpoStatus {

    PENDING("PENDING", "待收貨"),
    RECEIVED("RECEIVED", "已收貨"),
    DISCREPANCY("DISCREPANCY", "有差異");

    private final String code;
    private final String name;
}
