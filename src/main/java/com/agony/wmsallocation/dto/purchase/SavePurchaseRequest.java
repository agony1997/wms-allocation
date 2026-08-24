package com.agony.wmsallocation.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 儲存業務員訂貨明細的請求（一次送整張表格）。upsert 的業務鍵為
 * branchCode + locationCode + purchaseDate（每儲位每日一筆）；無單則建、有單則更新。
 * <p>{@code purchaseUser} 是此單所屬的**業務員（單主）**，非操作者——上司代訂時填被代訂的業務員，
 * 操作者身份屬 audit（createdBy），不進此欄。
 * <p>{@code purchaseDate} 的合法區間（D+2 ~ D+9）於 Service 驗；{@code productName} 由 Service 依 productCode 補齊。
 */
public record SavePurchaseRequest(
        @NotBlank @Size(max = 20) String branchCode,
        @NotBlank @Size(max = 20) String locationCode,
        @NotNull LocalDate purchaseDate,
        @NotBlank @Size(max = 20) String purchaseUser,
        @Valid @NotNull List<Detail> details) {

    public record Detail(
            @NotBlank @Size(max = 20) String productCode,
            @NotBlank @Size(max = 5) String unit,
            @NotNull @PositiveOrZero Integer qty,
            Integer sortOrder) {
    }
}
