package com.agony.wmsallocation.entity.allocation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 配貨/領貨狀態：標示配貨明細 (AOD) 是否已被業務員領取。
 * 業務員確認領貨後由 PENDING 流轉為 RECEIVED。
 */
@Getter
@RequiredArgsConstructor
public enum AllocationStatus {

    PENDING("PENDING", "待領取"),
    RECEIVED("RECEIVED", "已領取");

    private final String code;
    private final String name;
}
