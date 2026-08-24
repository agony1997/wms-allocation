package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.inventory.InventoryDto;
import com.agony.wmsallocation.dto.inventory.InventoryTransactionDto;
import com.agony.wmsallocation.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    // ==================== 即時庫存查詢 ====================

    /**
     * 查詢所有庫存
     */
    @GetMapping
    public List<InventoryDto> findAll() {
        return inventoryService.findAll();
    }

    /**
     * 查詢大庫庫存
     */
    @GetMapping("/warehouse/{branchCode}")
    public List<InventoryDto> findWarehouseInventory(@PathVariable String branchCode) {
        return inventoryService.findWarehouseInventory(branchCode);
    }

    /**
     * 查詢某儲位庫存
     */
    @GetMapping("/location/{locationCode}")
    public List<InventoryDto> findByLocation(@PathVariable String locationCode) {
        return inventoryService.findByLocation(locationCode);
    }

    /**
     * 查詢某產品庫存分布
     */
    @GetMapping("/product/{productCode}")
    public List<InventoryDto> findByProduct(@PathVariable String productCode) {
        return inventoryService.findByProduct(productCode);
    }

    // ==================== 異動記錄查詢 ====================

    /**
     * 依來源單據查詢異動記錄
     */
    @GetMapping("/transactions")
    public List<InventoryTransactionDto> findTransactions(@RequestParam String sourceDocType,
                                                          @RequestParam String sourceDocNo) {
        return inventoryService.findTransactionsByDoc(sourceDocType, sourceDocNo);
    }

    // ==================== 每日快照 ====================

    /**
     * 手動觸發當日快照。回 201 + Location 指向該日快照查詢端點（ADR-0003：成功回資源本體或
     * Location，不包訊息信封）；快照本體動輒整表，不隨建立回傳，需要時走 GET /snapshot/{date}。
     */
    @PostMapping("/snapshot")
    public ResponseEntity<Void> createSnapshot() {
        LocalDate snapshotDate = inventoryService.createTodaySnapshot();
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{date}")
                .buildAndExpand(snapshotDate)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * 查詢歷史快照
     */
    @GetMapping("/snapshot/{date}")
    public List<InventoryDto> findSnapshot(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                           @RequestParam(required = false) String branchCode) {
        if (branchCode != null) {
            return inventoryService.findSnapshotByDateAndBranch(date, branchCode);
        }
        return inventoryService.findSnapshotByDate(date);
    }

}
