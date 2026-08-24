package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDto;
import com.agony.wmsallocation.dto.purchase.SavePurchaseRequest;
import com.agony.wmsallocation.service.SalesPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales-purchase-orders")
public class SalesPurchaseController {

    private final SalesPurchaseService salesPurchaseService;

    // 唯讀查詢：無單回空白表單，不建資料（lazy create，見 ADR-0009）
    @GetMapping
    public SalesPurchaseOrderDto find(
            @RequestParam String branchCode,
            @RequestParam String locationCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        return salesPurchaseService.find(branchCode, locationCode, purchaseDate);
    }

    // upsert：業務鍵在 body（每儲位每日唯一），PUT 對該資源做建立或全量替換
    @PutMapping
    public SalesPurchaseOrderDto save(@Valid @RequestBody SavePurchaseRequest request) {
        return salesPurchaseService.save(request);
    }
}
