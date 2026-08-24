package com.agony.wmsallocation.entity.purchase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 業務員訂貨單明細 (SPOD) 狀態。單向流轉 PENDING → AGGREGATED → ALLOCATED：
 * 庫務將明細併入 BPO 後標記為 AGGREGATED（已送工廠）；配貨完成後標記為 ALLOCATED
 * （終態，防止重複配貨，見 allocation/AllocationOrder.md）。
 */
@Getter
@RequiredArgsConstructor
public enum SalesOrderDetailStatus {

    PENDING("PENDING", "待彙總"),
    AGGREGATED("AGGREGATED", "已彙總"),
    ALLOCATED("ALLOCATED", "已配貨");

    private final String code;
    private final String name;
}
