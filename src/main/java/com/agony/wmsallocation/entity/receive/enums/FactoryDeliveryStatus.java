package com.agony.wmsallocation.entity.receive.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 工廠出貨單 (FDO) 收貨狀態。PENDING 為工廠出貨後、營業所尚未收貨的初始狀態；
 * 庫務確認收貨時依實收與出貨數量是否相符，轉為 RECEIVED（相符）或 DISCREPANCY（有差異）。
 */
@Getter
@RequiredArgsConstructor
public enum FactoryDeliveryStatus {

    PENDING("PENDING", "待收貨"),
    RECEIVED("RECEIVED", "已收貨"),
    DISCREPANCY("DISCREPANCY", "有差異");

    private final String code;
    private final String name;
}
