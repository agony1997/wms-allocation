package com.agony.wmsallocation.dto.receive;

import com.agony.wmsallocation.entity.receive.enums.FactoryDeliveryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 工廠出貨單（單頭 + 明細）。出貨當下 receivedAt/receivedBy 為 null，收貨確認後才回填。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactoryDeliveryOrderDto {
    private String fdoNo;
    private String bpoNo;
    private String branchCode;
    private String factoryCode;
    private LocalDate deliveryDate;
    private FactoryDeliveryStatus status;
    private LocalDateTime receivedAt;
    private String receivedBy;
    private String remark;
    private List<FactoryDeliveryOrderDetailDto> details;
}
