package com.agony.wmsallocation.dto.inventory;

import com.agony.wmsallocation.entity.branch.enums.LocationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class InventoryDto {
    private Integer id;
    private String branchCode;
    private String locationCode;
    private LocationType locationType;
    private String productCode;
    private String batchNo;
    private LocalDate expiryDate;
    private Integer qty;
    private Integer keepQty;
    private Integer returnQty;
}
