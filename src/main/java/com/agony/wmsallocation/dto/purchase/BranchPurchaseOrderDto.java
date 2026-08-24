package com.agony.wmsallocation.dto.purchase;

import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 營業所訂貨單（單頭 + 明細），庫務彙總後的產出。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPurchaseOrderDto {
    private String bpoNo;
    private String branchCode;
    private String factoryCode;
    private LocalDate purchaseDate;
    private BpoStatus status;
    private List<BranchPurchaseOrderDetailDto> details;
}
