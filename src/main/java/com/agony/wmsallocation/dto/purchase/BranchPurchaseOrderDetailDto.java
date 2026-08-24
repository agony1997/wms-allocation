package com.agony.wmsallocation.dto.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPurchaseOrderDetailDto {
    private Integer itemNo;
    private String productCode;
    private String productName;
    private String unit;
    private Integer qty;
}
