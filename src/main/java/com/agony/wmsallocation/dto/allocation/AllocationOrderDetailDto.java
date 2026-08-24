package com.agony.wmsallocation.dto.allocation;

import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationOrderDetailDto {
    private String allocationNo;
    private Integer itemNo;
    private String locationCode;
    private String productCode;
    private String batchNo;
    private LocalDate expiryDate;
    private Integer requestedQty;
    private Integer allocatedQty;
    private AllocationStatus status;
}
