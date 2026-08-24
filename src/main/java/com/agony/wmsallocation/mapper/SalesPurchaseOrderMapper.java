package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDetailDto;
import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDto;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrderDetail;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 明細 Entity → DTO。單頭 DTO 需併入 editable、details，故在 Service 以 builder 手組。
 */
@Mapper(componentModel = "spring")
public interface SalesPurchaseOrderMapper {

    SalesPurchaseOrderDto toDto(SalesPurchaseOrder spo);

    SalesPurchaseOrderDetailDto toDetailDto(SalesPurchaseOrderDetail detail);

    List<SalesPurchaseOrderDetailDto> toDetailDtoList(List<SalesPurchaseOrderDetail> details);
}
