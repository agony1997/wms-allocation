package com.agony.wmsallocation.dto.allocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 業務領貨單（單頭 + 明細）。無狀態欄——SRO 一出生即為已領取
 * （見 {@code docs/requirements/specification/allocation/SalesReceiveOrder.md}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReceiveOrderDto {
    private String receiveNo;
    private String branchCode;
    private String locationCode;
    private LocalDate receiveDate;
    private List<SalesReceiveOrderDetailDto> details;
}
