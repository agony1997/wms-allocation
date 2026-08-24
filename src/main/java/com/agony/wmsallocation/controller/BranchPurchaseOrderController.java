package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.purchase.BranchPurchaseOrderDto;
import com.agony.wmsallocation.service.BranchPurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/branch-purchase-orders")
public class BranchPurchaseOrderController {

    private final BranchPurchaseOrderService branchPurchaseOrderService;

    @PostMapping("/actions/aggregate")
    public List<BranchPurchaseOrderDto> aggregate(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        return branchPurchaseOrderService.aggregate(branchCode, purchaseDate);
    }

    @GetMapping
    public List<BranchPurchaseOrderDto> list(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate) {
        return branchPurchaseOrderService.list(branchCode, purchaseDate);
    }
}
