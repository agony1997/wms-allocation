package com.agony.wmsallocation.dto.master;

import com.agony.wmsallocation.entity.enums.ActiveStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductDto {
    private String productCode;
    private String productName;
    private String baseUnit;
    private BigDecimal basePrice;
    private ActiveStatus status;
}
