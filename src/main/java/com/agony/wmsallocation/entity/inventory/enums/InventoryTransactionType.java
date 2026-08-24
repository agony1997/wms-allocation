package com.agony.wmsallocation.entity.inventory.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 庫存異動類型：標示每筆 InventoryTransaction 由何種業務操作產生。
 *
 * <p>各常數與來源單據的對照詳見
 * {@code docs/requirements/specification/master/Inventory.md}。
 */
@Getter
@RequiredArgsConstructor
public enum InventoryTransactionType {

    RECEIVE("RECEIVE", "收貨入庫"),
    ALLOCATE("ALLOCATE", "配貨扣庫"),
    PICK_UP("PICK_UP", "業務員領貨"),
    SALES("SALES", "銷售出庫"),
    CUSTOMER_RETURN("CUSTOMER_RETURN", "客戶退貨"),
    KEEP("KEEP", "寄庫"),
    KEEP_RETRIEVE("KEEP_RETRIEVE", "領回寄庫"),
    RETURN("RETURN", "退庫"),
    RETURN_SHIP("RETURN_SHIP", "銷退送出");

    private final String code;
    private final String name;
}
