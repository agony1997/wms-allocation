package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.master.CustomerDto;
import com.agony.wmsallocation.entity.master.Customer;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    CustomerDto toDto(Customer customer);

}
