package com.agony.wmsallocation.entity.branch.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 儲位類型：WAREHOUSE 為營業所大庫（locationCode = branchCode），CAR 為業務員車存。
 */
@Getter
@RequiredArgsConstructor
public enum LocationType {

    WAREHOUSE("WAREHOUSE", "倉庫"),
    CAR("CAR", "車輛");

    private final String code;
    private final String name;
}
