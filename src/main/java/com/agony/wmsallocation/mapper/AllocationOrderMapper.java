package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.allocation.AllocationOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.AllocationOrderDto;
import com.agony.wmsallocation.entity.allocation.AllocationOrder;
import com.agony.wmsallocation.entity.allocation.AllocationOrderDetail;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 明細 Entity → DTO。details 需另由 Service 組入，故單頭轉換不含明細。
 */
@Mapper(componentModel = "spring")
public interface AllocationOrderMapper {

    AllocationOrderDto toDto(AllocationOrder ao);

    AllocationOrderDetailDto toDetailDto(AllocationOrderDetail detail);

    List<AllocationOrderDetailDto> toDetailDtoList(List<AllocationOrderDetail> details);
}
