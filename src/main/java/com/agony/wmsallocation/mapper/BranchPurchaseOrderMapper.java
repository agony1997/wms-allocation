package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.purchase.BranchPurchaseOrderDetailDto;
import com.agony.wmsallocation.dto.purchase.BranchPurchaseOrderDto;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrderDetail;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 明細 Entity → DTO。details 需另由 Service 組入，故單頭轉換不含明細。
 */
@Mapper(componentModel = "spring")
public interface BranchPurchaseOrderMapper {

    BranchPurchaseOrderDto toDto(BranchPurchaseOrder bpo);

    BranchPurchaseOrderDetailDto toDetailDto(BranchPurchaseOrderDetail detail);

    List<BranchPurchaseOrderDetailDto> toDetailDtoList(List<BranchPurchaseOrderDetail> details);
}
