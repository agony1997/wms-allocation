package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.branch.BranchDto;
import com.agony.wmsallocation.entity.branch.Branch;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BranchMapper {

    BranchDto toDto(Branch branch);

}
