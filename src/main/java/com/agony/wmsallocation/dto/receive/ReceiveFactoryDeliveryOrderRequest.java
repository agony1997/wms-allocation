package com.agony.wmsallocation.dto.receive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * FDO 收貨確認請求（該 FDO 底下全部明細一次送出實收數量）。
 * {@code itemNo} 在單一 {@code fdoNo} 下已是唯一鍵，明細不需額外複合鍵定位。
 */
public record ReceiveFactoryDeliveryOrderRequest(
        @NotBlank @Size(max = 30) String fdoNo,
        @Size(max = 200) String remark,
        @Valid @NotEmpty List<Detail> details) {

    public record Detail(
            @NotNull Integer itemNo,
            @NotNull @PositiveOrZero Integer receivedQty) {
    }
}
