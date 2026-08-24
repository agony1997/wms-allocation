package com.agony.wmsallocation.dto.purchase;

import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPurchaseSummaryDto {
    private String branchCode;
    private LocalDate purchaseDate;
    private FrozenStatus frozenStatus;
    private List<SalesPurchaseOrderDto> orders;
}
