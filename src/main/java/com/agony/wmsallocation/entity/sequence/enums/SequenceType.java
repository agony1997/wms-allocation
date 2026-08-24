package com.agony.wmsallocation.entity.sequence.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 取號的單據類型；code 即單號前綴。清單須與
 * {@code docs/requirements/specification/SequenceNumber.md} 同步維護。
 *
 * <p>用 enum 而非資料表：新增一種單據類型必然伴隨新增 Entity/Service，
 * 不可能只在資料表加一列就生效，判斷準則見
 * {@code docs/adr/0007-closed-code-bound-sets-as-enum.md}。
 */
@Getter
@RequiredArgsConstructor
public enum SequenceType {

    // SalesPurchaseOrder
    SPO("SPO", "業務員訂貨單"),
    // BranchPurchaseFrozen
    BPF("BPF", "營業所凍結單"),
    // BranchPurchaseOrder
    BPO("BPO", "營業所訂貨單"),
    // FactoryDeliveryOrder
    FDO("FDO", "工廠出貨單"),
    // AllocationOrder
    AO("AO", "配貨單"),
    // SalesReceiveOrder
    SRO("SRO", "業務領貨單"),
    // CustomerPreOrder
    CPO("CPO", "客戶預訂單"),
    // SalesDeliveryOrder
    SDO("SDO", "送貨單"),
    // AccountReceivable
    AR("AR", "應收帳款"),
    // SalesKeepRecord
    SKR("SKR", "業務員寄庫單"),
    // SalesReturnRecord
    SRR("SRR", "業務員退庫單"),
    // BranchReturnOrder
    BRO("BRO", "營業所銷退單");

    private final String code;
    private final String name;
}
