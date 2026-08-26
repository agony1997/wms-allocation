package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDto;
import com.agony.wmsallocation.entity.allocation.SalesReceiveOrder;
import com.agony.wmsallocation.entity.allocation.SalesReceiveOrderDetail;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 明細 Entity → DTO。details 需另由 Service 組入，故單頭轉換不含明細（同 {@link AllocationOrderMapper}）。
 */
@Mapper(componentModel = "spring")
public interface SalesReceiveOrderMapper {

    SalesReceiveOrderDto toDto(SalesReceiveOrder sro);

    SalesReceiveOrderDetailDto toDetailDto(SalesReceiveOrderDetail detail);

    List<SalesReceiveOrderDetailDto> toDetailDtoList(List<SalesReceiveOrderDetail> details);
}
