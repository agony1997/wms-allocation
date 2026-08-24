package com.agony.wmsallocation.dto.allocation;

import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 配貨單（單頭 + 明細）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationOrderDto {
    private String allocationNo;
    private String branchCode;
    private LocalDate allocationDate;
    private AllocationStatus status;
    private List<AllocationOrderDetailDto> details;
}
