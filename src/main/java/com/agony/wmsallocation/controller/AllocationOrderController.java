package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.allocation.AllocationOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.AllocationOrderDto;
import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDetailDto;
import com.agony.wmsallocation.service.AllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/allocation-orders")
public class AllocationOrderController {

    private final AllocationService allocationService;

    /** 查詢某營業所待配貨的 SPOD（配貨前預覽）。 */
    @GetMapping("/pending-spod")
    public List<SalesPurchaseOrderDetailDto> listPendingSpod(@RequestParam String branchCode) {
        return allocationService.listPendingSpod(branchCode);
    }

    /** 執行配貨：依待配 SPOD 分配大庫庫存，回傳本次產生的配貨明細（無可配貨時回空清單）。 */
    @PostMapping("/actions/allocate")
    public List<AllocationOrderDetailDto> allocate(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate allocationDate) {
        return allocationService.executeAllocation(branchCode, allocationDate);
    }

    /** 查詢某營業所某日的配貨單清單。 */
    @GetMapping
    public List<AllocationOrderDto> list(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate allocationDate) {
        return allocationService.list(branchCode, allocationDate);
    }

    /** 查詢單一配貨單明細。 */
    @GetMapping("/{allocationNo}")
    public AllocationOrderDto get(@PathVariable String allocationNo) {
        return allocationService.get(allocationNo);
    }
}
