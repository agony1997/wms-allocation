package com.agony.wmsallocation.dto.purchase;

import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalesPurchaseOrderDetailDto {
    private String productCode;
    private String productName;
    private String unit;
    private int qty;
    private int confirmedQty;
    private int lastQty;
    private int sortOrder;
    private SalesOrderDetailStatus status;
}
