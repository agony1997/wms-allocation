package com.agony.wmsallocation.mapper;

import com.agony.wmsallocation.dto.master.ProductDto;
import com.agony.wmsallocation.entity.master.Product;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductDto toDto(Product product);

}
