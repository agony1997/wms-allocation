package com.agony.wmsallocation.dto.purchase;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 業務員訂貨單（單頭 + 明細 + 可否編輯旗標）。
 * <p>{@code editable} 由 BPF 狀態推導：BPF 不存在＝業務員可編輯。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesPurchaseOrderDto {
    private String purchaseNo;
    private String branchCode;
    private String locationCode;
    private LocalDate purchaseDate;
    private String purchaseUser;
    private boolean editable;
    private List<SalesPurchaseOrderDetailDto> details;
}
