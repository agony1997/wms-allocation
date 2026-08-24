package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.allocation.AllocationOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.AllocationOrderDto;
import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDetailDto;
import com.agony.wmsallocation.entity.allocation.AllocationOrder;
import com.agony.wmsallocation.entity.allocation.AllocationOrderDetail;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import com.agony.wmsallocation.entity.branch.SalesPriority;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.inventory.Inventory;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrderDetail;
import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.AllocationOrderMapper;
import com.agony.wmsallocation.mapper.SalesPurchaseOrderMapper;
import com.agony.wmsallocation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配貨 Service：selection（撈待配 SPOD、組 demand/batch）+ 落庫。
 * 分配演算法本身見 {@link AllocationCalculator}（純函式，不碰 DB）。
 * 業務規則詳見 {@code docs/requirements/specification/allocation/AllocationOrder.md}。
 */
@RequiredArgsConstructor
@Service
public class AllocationService {

    private final BranchPurchaseOrderRepo bpoRepo;
    private final SalesPurchaseOrderDetailRepo spodRepo;
    private final SalesPurchaseOrderRepo spoRepo;
    private final SalesPriorityRepo salesPriorityRepo;
    private final InventoryRepo inventoryRepo;
    private final AllocationOrderRepo aoRepo;
    private final AllocationOrderDetailRepo aodRepo;
    private final SequenceService sequenceService;
    private final InventoryService inventoryService;
    private final AllocationOrderMapper mapper;
    private final SalesPurchaseOrderMapper spodMapper;

    // 純函式、無狀態、刻意不掛 Spring（見其 javadoc），故直接 new，不走建構子注入
    private final AllocationCalculator allocationCalculator = new AllocationCalculator();

    public record ProductLocationKey(String productCode, String locationCode) {
    }

    /** 查詢某營業所待配貨的 SPOD（AGGREGATED 且對應 BPO 已收貨），供庫務配貨前預覽。 */
    public List<SalesPurchaseOrderDetailDto> listPendingSpod(String branchCode) {
        return spodMapper.toDetailDtoList(findPendingSpod(branchCode));
    }

    /** 查詢某營業所某日已產生的配貨單清單（單頭 + 明細）。 */
    public List<AllocationOrderDto> list(String branchCode, LocalDate allocationDate) {
        return aoRepo.findByBranchCodeAndAllocationDate(branchCode, allocationDate).stream()
                .map(ao -> {
                    AllocationOrderDto dto = mapper.toDto(ao);
                    dto.setDetails(mapper.toDetailDtoList(aodRepo.findByAllocationNoOrderByItemNo(ao.getAllocationNo())));
                    return dto;
                }).toList();
    }

    /** 查詢單一配貨單（單頭 + 明細）。 */
    public AllocationOrderDto get(String allocationNo) {
        AllocationOrder ao = aoRepo.findByAllocationNo(allocationNo)
                .orElseThrow(() -> new BusinessRuleException("配貨單不存在：" + allocationNo, ErrorCode.RESOURCE_NOT_FOUND));
        AllocationOrderDto dto = mapper.toDto(ao);
        dto.setDetails(mapper.toDetailDtoList(aodRepo.findByAllocationNoOrderByItemNo(allocationNo)));
        return dto;
    }

    /** 執行配貨，回傳本次產生的 AOD（供 Controller 用；不建 AO 時回空清單）。 */
    @Transactional
    public List<AllocationOrderDetailDto> executeAllocation(String branchCode, LocalDate allocationDate) {
        return mapper.toDetailDtoList(allocate(branchCode, allocationDate));
    }

    private List<SalesPurchaseOrderDetail> findPendingSpod(String branchCode) {
        // 撈該營業所已收貨 BPO 的單號（status IN RECEIVED, DISCREPANCY），再撈其下待配 SPOD（status = AGGREGATED）
        Set<String> bpoNos = bpoRepo.findByBranchCodeAndStatusIn(branchCode, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)).stream()
                .map(BranchPurchaseOrder::getBpoNo)
                .collect(Collectors.toSet());
        return spodRepo.findByBpoNoInAndStatus(bpoNos, SalesOrderDetailStatus.AGGREGATED);
    }

    @Transactional
    public List<AllocationOrderDetail> allocate(String branchCode, LocalDate allocationDate) {
        // 1-2. 待配 SPOD（AGGREGATED 且對應 BPO 已收貨）；空清單直接 return（冪等，不取號）
        List<SalesPurchaseOrderDetail> aggregatedOrderDetails = findPendingSpod(branchCode);
        if (aggregatedOrderDetails.isEmpty()) {
            return List.of();
        }

        // 3. 補 locationCode：SPOD 沒帶，經 distinct purchaseNo -> spoRepo.findByPurchaseNoIn
        //    組出 Map<purchaseNo, locationCode>
        Map<String, String> aggregatedOrderMap = spoRepo.findByPurchaseNoIn(aggregatedOrderDetails.stream()
                        .map(SalesPurchaseOrderDetail::getPurchaseNo)
                        .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(SalesPurchaseOrder::getPurchaseNo, SalesPurchaseOrder::getLocationCode));

        // 資料完整性守門：待配 SPOD 理論上彙總階段已保證 purchaseNo 有對應 SPO，查無則資料已不一致，
        // 明確中止並回報異常 purchaseNo，避免 locationCode=null 一路帶到 NOT NULL 欄位、以 DB 例外洩漏內部細節（審閱報告發現 2）
        aggregatedOrderDetails.stream()
                .map(SalesPurchaseOrderDetail::getPurchaseNo)
                .filter(purchaseNo -> !aggregatedOrderMap.containsKey(purchaseNo))
                .findFirst()
                .ifPresent(purchaseNo -> {
                    throw new BusinessRuleException("待配 SPOD 查無對應訂貨單：" + purchaseNo, ErrorCode.PURCHASE_ORDER_NOT_FOUND);
                });

        // 4. 建優先度 map：salesPriorityRepo.findByBranchCode -> Map<locationCode, priorityLevel>
        //    （查無資料的 locationCode 不放進 map，calculator 對 null 視為最低優先）
        Map<String, Integer> priorityLevelMap = salesPriorityRepo.findByBranchCode(branchCode).stream()
                .collect(Collectors.toMap(SalesPriority::getLocationCode, SalesPriority::getPriorityLevel));

        // 5. 組 demand / batch，皆按 productCode 分組：
        //    - demand：同產品同業務員多筆 SPOD 的 confirmedQty 加總成一筆 AllocationDemand
        //    - batch：inventoryRepo.findByBranchCodeAndLocationType(branchCode, WAREHOUSE) 依 productCode 過濾
        Map<ProductLocationKey, Integer> requestQtyMap = aggregatedOrderDetails.stream()
                .collect(Collectors.groupingBy(detail -> new ProductLocationKey(detail.getProductCode(), aggregatedOrderMap.get(detail.getPurchaseNo())),
                        Collectors.summingInt(SalesPurchaseOrderDetail::getConfirmedQty)));

        Map<String, List<AllocationCalculator.AllocationDemand>> demandsMap = requestQtyMap.entrySet().stream()
                .collect(Collectors.groupingBy(entry -> entry.getKey().productCode(),
                        Collectors.mapping(entry ->
                                        new AllocationCalculator.AllocationDemand(
                                                entry.getKey().locationCode(),
                                                priorityLevelMap.get(entry.getKey().locationCode()),
                                                entry.getValue()),
                                Collectors.toList())));

        Map<String, List<AllocationCalculator.BatchStock>> batchesMap = inventoryRepo.findByBranchCodeAndLocationType(branchCode, LocationType.WAREHOUSE).stream()
                .filter(i -> i.getQty() > 0)
                .collect(Collectors.groupingBy(Inventory::getProductCode,
                        Collectors.mapping(i -> new AllocationCalculator.BatchStock(
                                        i.getBatchNo(),
                                        i.getExpiryDate(),
                                        i.getQty()),
                                Collectors.toList())));

        // 6. 取號建 AO -> 逐產品呼叫 allocationCalculator.allocate(demands, batches) -> 建 AOD（itemNo 跨產品連續）
        //    -> 對每筆 AOD 呼叫 inventoryService.allocate(...) 扣大庫（走 ADR-0013 悲觀鎖，併入本交易）
        //    -> 待配 SPOD 全部轉 ALLOCATED（含分到 0 的，防重複配貨）
        String sequence = sequenceService.generateSequence(SequenceType.AO, allocationDate);
        AllocationOrder allocationOrder = new AllocationOrder();
        allocationOrder.setAllocationNo(sequence);
        allocationOrder.setBranchCode(branchCode);
        allocationOrder.setAllocationDate(allocationDate);
        allocationOrder.setStatus(AllocationStatus.PENDING);
        aoRepo.save(allocationOrder);

        List<AllocationOrderDetail> details = new ArrayList<>();
        int itemNo = 1;

        for (var entry : demandsMap.entrySet()) {
            String productCode = entry.getKey();
            for (var line : allocationCalculator.allocate(entry.getValue(), batchesMap.getOrDefault(productCode, Collections.emptyList()))) {
                AllocationOrderDetail detail = new AllocationOrderDetail();
                detail.setAllocationNo(sequence);
                detail.setLocationCode(line.locationCode());
                detail.setProductCode(productCode);
                detail.setBatchNo(line.batchNo());
                detail.setExpiryDate(line.expiryDate());
                detail.setAllocatedQty(line.allocatedQty());
                detail.setRequestedQty(line.requestedQty());
                detail.setStatus(AllocationStatus.PENDING);
                detail.setItemNo(itemNo++);
                details.add(detail);

                inventoryService.allocate(branchCode, detail.getProductCode(), detail.getBatchNo(), detail.getExpiryDate(), detail.getAllocatedQty(), detail.getAllocationNo());
            }
        }


        aggregatedOrderDetails.forEach(detail -> detail.setStatus(SalesOrderDetailStatus.ALLOCATED));

        aodRepo.saveAll(details);
        spodRepo.saveAll(aggregatedOrderDetails);

        return details;
    }
}
