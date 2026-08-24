package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.receive.FactoryDeliveryOrderDetailDto;
import com.agony.wmsallocation.dto.receive.FactoryDeliveryOrderDto;
import com.agony.wmsallocation.entity.receive.FactoryDeliveryOrder;
import com.agony.wmsallocation.entity.receive.FactoryDeliveryOrderDetail;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * FDO Entity → DTO。details 需另由 Service 組入，故單頭轉換不含明細。
 */
@Mapper(componentModel = "spring")
public interface FactoryDeliveryOrderMapper {

    FactoryDeliveryOrderDto toDto(FactoryDeliveryOrder fdo);

    FactoryDeliveryOrderDetailDto toDetailDto(FactoryDeliveryOrderDetail detail);

    List<FactoryDeliveryOrderDetailDto> toDetailDtoList(List<FactoryDeliveryOrderDetail> details);
}
