package com.agony.wmsallocation.dto.inventory;

import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.inventory.enums.InventoryTransactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryTransactionDto {
    private Integer id;
    private String branchCode;
    private String locationCode;
    private LocationType locationType;
    private String productCode;
    private String batchNo;
    private LocalDate expiryDate;
    private InventoryTransactionType transactionType;
    private Integer qtyChange;
    private Integer keepQtyChange;
    private Integer returnQtyChange;
    private String sourceDocType;
    private String sourceDocNo;
    private LocalDateTime createdAt;
    private String createdBy;
}
