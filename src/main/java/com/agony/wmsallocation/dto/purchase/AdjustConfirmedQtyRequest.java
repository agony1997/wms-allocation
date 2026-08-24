package com.agony.wmsallocation.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AdjustConfirmedQtyRequest {

    @NotEmpty
    @Valid
    private List<Detail> adjustments;

    public record Detail(
            @NotNull String locationCode,
            @NotNull String productCode,
            @NotNull String unit,
            @NotNull @Min(0) Integer confirmedQty
    ) {}
}
