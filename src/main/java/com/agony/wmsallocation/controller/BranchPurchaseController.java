package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.purchase.AdjustConfirmedQtyRequest;
import com.agony.wmsallocation.dto.purchase.BranchPurchaseSummaryDto;
import com.agony.wmsallocation.service.BranchPurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/branch-purchases")
public class BranchPurchaseController {

    private final BranchPurchaseService branchPurchaseService;

    // 取得該營業所當天的訂單網格資訊（以 Flat List 形式回傳，前端自行轉 Matrix）
    @GetMapping
    public BranchPurchaseSummaryDto getBranchSummary(
            @RequestParam String branchCode, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        return branchPurchaseService.getBranchSummary(branchCode, purchaseDate);
    }

    // 操作者身份由 Service 從 UserContextHolder（JwtInterceptor 寫入）取得，不開放呼叫端指定
    @PostMapping("/actions/freeze")
    public void freezeBranchPurchase(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        branchPurchaseService.freeze(branchCode, purchaseDate);
    }

    @PostMapping("/actions/unfreeze")
    public void unfreezeBranchPurchase(
            @RequestParam String branchCode, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        branchPurchaseService.unfreeze(branchCode, purchaseDate);
    }

    @PostMapping("/actions/confirm")
    public void confirmBranchPurchase(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        branchPurchaseService.confirm(branchCode, purchaseDate);
    }

    @PutMapping("/adjust")
    public void adjustConfirmedQty(
            @RequestParam String branchCode, 
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
            @Valid @RequestBody AdjustConfirmedQtyRequest request) {
        branchPurchaseService.adjustConfirmedQty(branchCode, purchaseDate, request);
    }
}
