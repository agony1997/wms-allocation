package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.auth.UserDto;
import com.agony.wmsallocation.entity.auth.AuthUser;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthUserMapper {

    // ponytail: password 不在 UserDto，MapStruct 自動跳過，無需 @Mapping(target="password", ignore=true)
    UserDto toDto(AuthUser authUser);

}
