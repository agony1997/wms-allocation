package com.agony.wmsallocation.dto.allocation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesReceiveOrderDetailDto {
    private String receiveNo;
    private Integer itemNo;
    /** 與 allocationItemNo 共同指向來源 AOD。 */
    private String allocationNo;
    private Integer allocationItemNo;
    private String productCode;
    private String batchNo;
    private LocalDate expiryDate;
    private Integer qty;
}
