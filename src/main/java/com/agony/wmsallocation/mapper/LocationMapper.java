package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.branch.LocationDto;
import com.agony.wmsallocation.entity.branch.Location;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationDto toDto(Location location);

}
