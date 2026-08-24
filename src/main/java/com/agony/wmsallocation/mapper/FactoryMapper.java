package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.master.FactoryDto;
import com.agony.wmsallocation.entity.master.Factory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FactoryMapper {

    FactoryDto toDto(Factory factory);

}
