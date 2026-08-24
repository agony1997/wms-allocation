package com.agony.wmsallocation.entity.purchase.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 營業所凍結單 (BPF) 狀態。流轉為單向 FROZEN → CONFIRMED；
 * 確認前可由組長解除凍結（刪除 BPF 回到開放），CONFIRMED 後不可逆。
 */
@Getter
@RequiredArgsConstructor
public enum FrozenStatus {

    FROZEN("FROZEN", "已凍結"),
    CONFIRMED("CONFIRMED", "已確認");

    private final String code;
    private final String name;
}
