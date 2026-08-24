package com.agony.wmsallocation.dto.receive;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryDeliveryOrderDetailDto {
    private Integer itemNo;
    private String productCode;
    private String productName;
    private String batchNo;
    private LocalDate expiryDate;
    private String unit;
    private Integer qty;
    private Integer receivedQty;
}
