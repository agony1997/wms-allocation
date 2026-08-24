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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * 配貨 Service 的 selection + 落庫測試（test-first，ADR-0005：真實業務規則）。
 * 分配演算法本身（FEFO/優先度/跨批拆分）已由 {@link AllocationCalculatorTest} 覆蓋，
 * 這裡只釘住 Service 層特有的行為：撈資料組 demand/batch、冪等、優先度缺值 fallback、
 * 分 0 仍轉態防重複配貨、itemNo 跨產品連續。
 */
@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    private static final String BRANCH = "B001";
    private static final LocalDate ALLOCATION_DATE = LocalDate.of(2026, 7, 15);

    @Mock private BranchPurchaseOrderRepo bpoRepo;
    @Mock private SalesPurchaseOrderDetailRepo spodRepo;
    @Mock private SalesPurchaseOrderRepo spoRepo;
    @Mock private SalesPriorityRepo salesPriorityRepo;
    @Mock private InventoryRepo inventoryRepo;
    @Mock private AllocationOrderRepo aoRepo;
    @Mock private AllocationOrderDetailRepo aodRepo;
    @Mock private SequenceService sequenceService;
    @Mock private InventoryService inventoryService;
    @Mock private AllocationOrderMapper mapper;
    @Mock private SalesPurchaseOrderMapper spodMapper;

    @InjectMocks
    private AllocationService allocationService;

    private BranchPurchaseOrder bpoOf(String bpoNo) {
        BranchPurchaseOrder bpo = new BranchPurchaseOrder();
        bpo.setBpoNo(bpoNo);
        return bpo;
    }

    private SalesPurchaseOrder spoOf(String purchaseNo, String locationCode) {
        SalesPurchaseOrder spo = new SalesPurchaseOrder();
        spo.setPurchaseNo(purchaseNo);
        spo.setLocationCode(locationCode);
        return spo;
    }

    private SalesPurchaseOrderDetail spodOf(String purchaseNo, String productCode, int confirmedQty, String bpoNo) {
        SalesPurchaseOrderDetail spod = new SalesPurchaseOrderDetail();
        spod.setPurchaseNo(purchaseNo);
        spod.setProductCode(productCode);
        spod.setConfirmedQty(confirmedQty);
        spod.setBpoNo(bpoNo);
        spod.setStatus(SalesOrderDetailStatus.AGGREGATED);
        return spod;
    }

    private Inventory warehouseInventory(String productCode, String batchNo, LocalDate expiryDate, int qty) {
        Inventory inv = new Inventory();
        inv.setProductCode(productCode);
        inv.setBatchNo(batchNo);
        inv.setExpiryDate(expiryDate);
        inv.setQty(qty);
        return inv;
    }

    private SalesPriority priorityOf(String locationCode, int level) {
        SalesPriority priority = new SalesPriority();
        priority.setLocationCode(locationCode);
        priority.setPriorityLevel(level);
        return priority;
    }

    private AllocationOrder allocationOrderOf(String allocationNo, String branchCode, LocalDate allocationDate) {
        AllocationOrder ao = new AllocationOrder();
        ao.setAllocationNo(allocationNo);
        ao.setBranchCode(branchCode);
        ao.setAllocationDate(allocationDate);
        ao.setStatus(AllocationStatus.PENDING);
        return ao;
    }

    private AllocationOrderDetail allocationOrderDetailOf(String allocationNo, int itemNo) {
        AllocationOrderDetail detail = new AllocationOrderDetail();
        detail.setAllocationNo(allocationNo);
        detail.setItemNo(itemNo);
        detail.setStatus(AllocationStatus.PENDING);
        return detail;
    }

    @Test
    @DisplayName("無待配 SPOD - 應回空清單且不取號建 AO（冪等）")
    void allocate_whenNoAggregatedSpod_returnsEmptyWithoutGeneratingSequence() {
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of());

        List<AllocationOrderDetail> result = allocationService.allocate(BRANCH, ALLOCATION_DATE);

        assertThat(result).isEmpty();
        verify(sequenceService, never()).generateSequence(any(), any());
        verify(aoRepo, never()).save(any());
    }

    @Test
    @DisplayName("單一業務員單一批次 - 應建 AOD、扣大庫、SPOD 轉 ALLOCATED")
    void allocate_happyPath_singleDemandSingleBatch_createsAodAndDeductsInventoryAndMarksSpodAllocated() {
        LocalDate expiry = LocalDate.of(2026, 12, 31);
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));

        SalesPurchaseOrderDetail spod = spodOf("SPO001", "P001", 10, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spod));
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001"))).thenReturn(List.of(spoOf("SPO001", "S001")));
        when(salesPriorityRepo.findByBranchCode(BRANCH)).thenReturn(List.of(priorityOf("S001", 1)));
        when(inventoryRepo.findByBranchCodeAndLocationType(BRANCH, LocationType.WAREHOUSE))
                .thenReturn(List.of(warehouseInventory("P001", "BATCH01", expiry, 20)));
        when(sequenceService.generateSequence(SequenceType.AO, ALLOCATION_DATE)).thenReturn("AO-20260715-001");

        List<AllocationOrderDetail> result = allocationService.allocate(BRANCH, ALLOCATION_DATE);

        assertThat(result).hasSize(1);
        AllocationOrderDetail detail = result.get(0);
        assertThat(detail.getAllocationNo()).isEqualTo("AO-20260715-001");
        assertThat(detail.getItemNo()).isEqualTo(1);
        assertThat(detail.getLocationCode()).isEqualTo("S001");
        assertThat(detail.getProductCode()).isEqualTo("P001");
        assertThat(detail.getBatchNo()).isEqualTo("BATCH01");
        assertThat(detail.getExpiryDate()).isEqualTo(expiry);
        assertThat(detail.getRequestedQty()).isEqualTo(10);
        assertThat(detail.getAllocatedQty()).isEqualTo(10);
        assertThat(detail.getStatus()).isEqualTo(AllocationStatus.PENDING);
        assertThat(spod.getStatus()).isEqualTo(SalesOrderDetailStatus.ALLOCATED);

        verify(inventoryService).allocate(BRANCH, "P001", "BATCH01", expiry, 10, "AO-20260715-001");

        ArgumentCaptor<AllocationOrder> aoCaptor = ArgumentCaptor.forClass(AllocationOrder.class);
        verify(aoRepo).save(aoCaptor.capture());
        assertThat(aoCaptor.getValue().getAllocationNo()).isEqualTo("AO-20260715-001");
        assertThat(aoCaptor.getValue().getBranchCode()).isEqualTo(BRANCH);
        assertThat(aoCaptor.getValue().getAllocationDate()).isEqualTo(ALLOCATION_DATE);
        assertThat(aoCaptor.getValue().getStatus()).isEqualTo(AllocationStatus.PENDING);
    }

    @Test
    @DisplayName("同產品同業務員多筆 SPOD - confirmedQty 應加總成一筆需求")
    void allocate_multipleSpodSameProductAndLocation_sumsConfirmedQtyIntoOneDemand() {
        LocalDate expiry = LocalDate.of(2026, 12, 31);
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));

        SalesPurchaseOrderDetail spod1 = spodOf("SPO001", "P001", 5, "BPO001");
        SalesPurchaseOrderDetail spod2 = spodOf("SPO002", "P001", 3, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spod1, spod2));
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001", "SPO002")))
                .thenReturn(List.of(spoOf("SPO001", "S001"), spoOf("SPO002", "S001")));
        when(salesPriorityRepo.findByBranchCode(BRANCH)).thenReturn(List.of(priorityOf("S001", 1)));
        when(inventoryRepo.findByBranchCodeAndLocationType(BRANCH, LocationType.WAREHOUSE))
                .thenReturn(List.of(warehouseInventory("P001", "BATCH01", expiry, 20)));
        when(sequenceService.generateSequence(SequenceType.AO, ALLOCATION_DATE)).thenReturn("AO-20260715-001");

        List<AllocationOrderDetail> result = allocationService.allocate(BRANCH, ALLOCATION_DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRequestedQty()).isEqualTo(8);
        assertThat(result.get(0).getAllocatedQty()).isEqualTo(8);
        assertThat(spod1.getStatus()).isEqualTo(SalesOrderDetailStatus.ALLOCATED);
        assertThat(spod2.getStatus()).isEqualTo(SalesOrderDetailStatus.ALLOCATED);
    }

    @Test
    @DisplayName("業務員查無優先度資料 - 視為最低優先，庫存不足時排在有優先度的後面且不建 AOD")
    void allocate_locationWithoutPriorityRecord_treatedAsLowestPriority() {
        LocalDate expiry = LocalDate.of(2026, 12, 31);
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));

        SalesPurchaseOrderDetail spodWithPriority = spodOf("SPO001", "P001", 10, "BPO001");
        SalesPurchaseOrderDetail spodNoPriority = spodOf("SPO002", "P001", 10, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spodWithPriority, spodNoPriority));
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001", "SPO002")))
                .thenReturn(List.of(spoOf("SPO001", "S001"), spoOf("SPO002", "S002")));
        // 只有 S001 有優先度資料，S002 查無
        when(salesPriorityRepo.findByBranchCode(BRANCH)).thenReturn(List.of(priorityOf("S001", 1)));
        when(inventoryRepo.findByBranchCodeAndLocationType(BRANCH, LocationType.WAREHOUSE))
                .thenReturn(List.of(warehouseInventory("P001", "BATCH01", expiry, 10)));
        when(sequenceService.generateSequence(SequenceType.AO, ALLOCATION_DATE)).thenReturn("AO-20260715-001");

        List<AllocationOrderDetail> result = allocationService.allocate(BRANCH, ALLOCATION_DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocationCode()).isEqualTo("S001");
        assertThat(result.get(0).getAllocatedQty()).isEqualTo(10);
        assertThat(spodWithPriority.getStatus()).isEqualTo(SalesOrderDetailStatus.ALLOCATED);
        // S002 沒排到貨、沒有 AOD，但 SPOD 仍轉 ALLOCATED（防重複配貨）
        assertThat(spodNoPriority.getStatus()).isEqualTo(SalesOrderDetailStatus.ALLOCATED);
        verify(inventoryService, times(1)).allocate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("完全無庫存可分配 - 不建 AOD，但 SPOD 仍轉 ALLOCATED 防止重複配貨")
    void allocate_noStockAvailable_createsNoAodButStillMarksSpodAllocated() {
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));

        SalesPurchaseOrderDetail spod = spodOf("SPO001", "P002", 10, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spod));
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001"))).thenReturn(List.of(spoOf("SPO001", "S001")));
        when(salesPriorityRepo.findByBranchCode(BRANCH)).thenReturn(List.of(priorityOf("S001", 1)));
        when(inventoryRepo.findByBranchCodeAndLocationType(BRANCH, LocationType.WAREHOUSE)).thenReturn(List.of());
        when(sequenceService.generateSequence(SequenceType.AO, ALLOCATION_DATE)).thenReturn("AO-20260715-001");

        List<AllocationOrderDetail> result = allocationService.allocate(BRANCH, ALLOCATION_DATE);

        assertThat(result).isEmpty();
        assertThat(spod.getStatus()).isEqualTo(SalesOrderDetailStatus.ALLOCATED);
        verify(inventoryService, never()).allocate(any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    @DisplayName("待配 SPOD 查無對應訂貨單 - 應拋 BusinessRuleException 中止配貨（資料一致性守門）")
    void allocate_purchaseOrderNotFound_throwsBusinessRuleException() {
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));

        SalesPurchaseOrderDetail spod = spodOf("SPO001", "P001", 10, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spod));
        // SPO001 查無對應 SalesPurchaseOrder
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001"))).thenReturn(List.of());

        assertThatThrownBy(() -> allocationService.allocate(BRANCH, ALLOCATION_DATE))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PURCHASE_ORDER_NOT_FOUND));
        verify(aodRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("多產品同時配貨 - itemNo 應跨產品連續不重複")
    void allocate_multipleProducts_itemNoIsContinuousAndUniqueAcrossAllDetails() {
        LocalDate expiry = LocalDate.of(2026, 12, 31);
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));

        SalesPurchaseOrderDetail spodP1 = spodOf("SPO001", "P001", 5, "BPO001");
        SalesPurchaseOrderDetail spodP2 = spodOf("SPO001", "P002", 5, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spodP1, spodP2));
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001"))).thenReturn(List.of(spoOf("SPO001", "S001")));
        when(salesPriorityRepo.findByBranchCode(BRANCH)).thenReturn(List.of(priorityOf("S001", 1)));
        // 每個產品各兩批、各自需拆成 2 筆 AOD，總共 4 筆
        when(inventoryRepo.findByBranchCodeAndLocationType(BRANCH, LocationType.WAREHOUSE))
                .thenReturn(List.of(
                        warehouseInventory("P001", "BATCH01", expiry, 3),
                        warehouseInventory("P001", "BATCH02", expiry.plusDays(1), 3),
                        warehouseInventory("P002", "BATCH01", expiry, 3),
                        warehouseInventory("P002", "BATCH02", expiry.plusDays(1), 3)));
        when(sequenceService.generateSequence(SequenceType.AO, ALLOCATION_DATE)).thenReturn("AO-20260715-001");

        List<AllocationOrderDetail> result = allocationService.allocate(BRANCH, ALLOCATION_DATE);

        assertThat(result).hasSize(4);
        assertThat(result.stream().map(AllocationOrderDetail::getItemNo).sorted().toList())
                .containsExactly(1, 2, 3, 4);
        assertThat(result.stream().map(AllocationOrderDetail::getItemNo).distinct().count()).isEqualTo(4);
    }

    @Test
    @DisplayName("listPendingSpod - 應撈待配 SPOD 並轉為 DTO")
    void listPendingSpod_returnsMappedDtoList() {
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));
        SalesPurchaseOrderDetail spod = spodOf("SPO001", "P001", 10, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spod));
        SalesPurchaseOrderDetailDto dto = SalesPurchaseOrderDetailDto.builder()
                .productCode("P001")
                .confirmedQty(10)
                .status(SalesOrderDetailStatus.AGGREGATED)
                .build();
        when(spodMapper.toDetailDtoList(List.of(spod))).thenReturn(List.of(dto));

        List<SalesPurchaseOrderDetailDto> result = allocationService.listPendingSpod(BRANCH);

        assertThat(result).containsExactly(dto);
    }

    @Test
    @DisplayName("list - 應查出配貨單清單且各自帶對應明細（不同 AO 的明細不會混在一起）")
    void list_returnsOrdersEachWithOwnDetails() {
        AllocationOrder ao1 = allocationOrderOf("AO-001", BRANCH, ALLOCATION_DATE);
        AllocationOrder ao2 = allocationOrderOf("AO-002", BRANCH, ALLOCATION_DATE);
        when(aoRepo.findByBranchCodeAndAllocationDate(BRANCH, ALLOCATION_DATE)).thenReturn(List.of(ao1, ao2));

        AllocationOrderDetail detail1 = allocationOrderDetailOf("AO-001", 1);
        AllocationOrderDetail detail2 = allocationOrderDetailOf("AO-002", 1);
        when(aodRepo.findByAllocationNoOrderByItemNo("AO-001")).thenReturn(List.of(detail1));
        when(aodRepo.findByAllocationNoOrderByItemNo("AO-002")).thenReturn(List.of(detail2));

        AllocationOrderDto dto1 = AllocationOrderDto.builder().allocationNo("AO-001").build();
        AllocationOrderDto dto2 = AllocationOrderDto.builder().allocationNo("AO-002").build();
        when(mapper.toDto(ao1)).thenReturn(dto1);
        when(mapper.toDto(ao2)).thenReturn(dto2);

        AllocationOrderDetailDto detailDto1 = AllocationOrderDetailDto.builder().allocationNo("AO-001").itemNo(1).build();
        AllocationOrderDetailDto detailDto2 = AllocationOrderDetailDto.builder().allocationNo("AO-002").itemNo(1).build();
        when(mapper.toDetailDtoList(List.of(detail1))).thenReturn(List.of(detailDto1));
        when(mapper.toDetailDtoList(List.of(detail2))).thenReturn(List.of(detailDto2));

        List<AllocationOrderDto> result = allocationService.list(BRANCH, ALLOCATION_DATE);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDetails()).containsExactly(detailDto1);
        assertThat(result.get(1).getDetails()).containsExactly(detailDto2);
    }

    @Test
    @DisplayName("get - 查得到時應回傳配貨單並帶明細")
    void get_whenFound_returnsDtoWithDetails() {
        AllocationOrder ao = allocationOrderOf("AO-001", BRANCH, ALLOCATION_DATE);
        when(aoRepo.findByAllocationNo("AO-001")).thenReturn(Optional.of(ao));

        AllocationOrderDetail detail = allocationOrderDetailOf("AO-001", 1);
        when(aodRepo.findByAllocationNoOrderByItemNo("AO-001")).thenReturn(List.of(detail));

        AllocationOrderDto dto = AllocationOrderDto.builder().allocationNo("AO-001").build();
        when(mapper.toDto(ao)).thenReturn(dto);
        AllocationOrderDetailDto detailDto = AllocationOrderDetailDto.builder().allocationNo("AO-001").itemNo(1).build();
        when(mapper.toDetailDtoList(List.of(detail))).thenReturn(List.of(detailDto));

        AllocationOrderDto result = allocationService.get("AO-001");

        assertThat(result.getDetails()).containsExactly(detailDto);
    }

    @Test
    @DisplayName("get - 查無配貨單應拋 BusinessRuleException(RESOURCE_NOT_FOUND)")
    void get_whenNotFound_throwsBusinessRuleException() {
        when(aoRepo.findByAllocationNo("AO-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allocationService.get("AO-404"))
                .isInstanceOfSatisfying(BusinessRuleException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("executeAllocation - 應執行配貨並轉為 DTO 清單")
    void executeAllocation_returnsMappedDetailDtos() {
        LocalDate expiry = LocalDate.of(2026, 12, 31);
        when(bpoRepo.findByBranchCodeAndStatusIn(BRANCH, Set.of(BpoStatus.RECEIVED, BpoStatus.DISCREPANCY)))
                .thenReturn(List.of(bpoOf("BPO001")));
        SalesPurchaseOrderDetail spod = spodOf("SPO001", "P001", 10, "BPO001");
        when(spodRepo.findByBpoNoInAndStatus(Set.of("BPO001"), SalesOrderDetailStatus.AGGREGATED))
                .thenReturn(List.of(spod));
        when(spoRepo.findByPurchaseNoIn(Set.of("SPO001"))).thenReturn(List.of(spoOf("SPO001", "S001")));
        when(salesPriorityRepo.findByBranchCode(BRANCH)).thenReturn(List.of(priorityOf("S001", 1)));
        when(inventoryRepo.findByBranchCodeAndLocationType(BRANCH, LocationType.WAREHOUSE))
                .thenReturn(List.of(warehouseInventory("P001", "BATCH01", expiry, 20)));
        when(sequenceService.generateSequence(SequenceType.AO, ALLOCATION_DATE)).thenReturn("AO-20260715-001");

        AllocationOrderDetailDto mappedDto = AllocationOrderDetailDto.builder()
                .allocationNo("AO-20260715-001")
                .itemNo(1)
                .productCode("P001")
                .build();
        when(mapper.toDetailDtoList(any())).thenReturn(List.of(mappedDto));

        List<AllocationOrderDetailDto> result = allocationService.executeAllocation(BRANCH, ALLOCATION_DATE);

        assertThat(result).containsExactly(mappedDto);
    }
}
