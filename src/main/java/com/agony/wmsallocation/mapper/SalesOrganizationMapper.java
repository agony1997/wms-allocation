package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.master.SalesOrganizationDto;
import com.agony.wmsallocation.entity.master.SalesOrganization;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SalesOrganizationMapper {

    SalesOrganizationDto toDto(SalesOrganization salesOrganization);

}
