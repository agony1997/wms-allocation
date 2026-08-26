package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDetailDto;
import com.agony.wmsallocation.entity.allocation.AllocationOrder;
import com.agony.wmsallocation.entity.allocation.AllocationOrderDetail;
import com.agony.wmsallocation.entity.allocation.SalesReceiveOrder;
import com.agony.wmsallocation.entity.allocation.SalesReceiveOrderDetail;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import com.agony.wmsallocation.entity.branch.Location;
import com.agony.wmsallocation.entity.branch.enums.LocationType;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.AllocationOrderMapper;
import com.agony.wmsallocation.mapper.SalesReceiveOrderMapper;
import com.agony.wmsallocation.repository.AllocationOrderDetailRepo;
import com.agony.wmsallocation.repository.AllocationOrderRepo;
import com.agony.wmsallocation.repository.LocationRepo;
import com.agony.wmsallocation.repository.SalesReceiveOrderDetailRepo;
import com.agony.wmsallocation.repository.SalesReceiveOrderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 領貨 Service 測試（test-first，ADR-0005：狀態機＋庫存屬真實業務規則）。
 *
 * <p>釘住規格明說的四件事：
 * <ul>
 *   <li>只收 locationCode（全域唯一），branchCode 由儲位主檔反查、不由呼叫端指定</li>
 *   <li>領貨只加車存，大庫已在配貨時預留扣除（SalesReceiveOrder.md 庫存影響）</li>
 *   <li>AO 聚合狀態：一張 AO 底下的 AOD 分屬多個業務員，須全部領完才轉 RECEIVED
 *       （AllocationOrder.md 狀態約束）</li>
 *   <li>無待領明細時冪等返回，不取號、不建單</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class SalesReceiveOrderServiceTest {

    private static final String BRANCH = "1000";
    private static final String LOCATION = "1011";
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 20);
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-20T01:00:00Z"), ZoneOffset.UTC);
    private static final String RECEIVE_NO = "SRO-20260720-001";

    @Mock private AllocationOrderRepo aoRepo;
    @Mock private AllocationOrderDetailRepo aodRepo;
    @Mock private SalesReceiveOrderRepo sroRepo;
    @Mock private SalesReceiveOrderDetailRepo srodRepo;
    @Mock private LocationRepo locationRepo;
    @Mock private SequenceService sequenceService;
    @Mock private InventoryService inventoryService;
    @Mock private SalesReceiveOrderMapper mapper;
    @Mock private AllocationOrderMapper allocationOrderMapper;

    private SalesReceiveOrderService service;

    @BeforeEach
    void setUp() {
        service = new SalesReceiveOrderService(aoRepo, aodRepo, sroRepo, srodRepo, locationRepo,
                sequenceService, inventoryService, mapper, allocationOrderMapper, FIXED_CLOCK);
    }

    private AllocationOrder ao(String allocationNo) {
        AllocationOrder ao = new AllocationOrder();
        ao.setAllocationNo(allocationNo);
        ao.setBranchCode(BRANCH);
        ao.setAllocationDate(TODAY);
        ao.setStatus(AllocationStatus.PENDING);
        return ao;
    }

    private AllocationOrderDetail aod(String allocationNo, int itemNo, String productCode,
                                      String batchNo, int allocatedQty) {
        AllocationOrderDetail d = new AllocationOrderDetail();
        d.setAllocationNo(allocationNo);
        d.setItemNo(itemNo);
        d.setLocationCode(LOCATION);
        d.setProductCode(productCode);
        d.setBatchNo(batchNo);
        d.setExpiryDate(LocalDate.of(2026, 12, 31));
        d.setRequestedQty(allocatedQty);
        d.setAllocatedQty(allocatedQty);
        d.setStatus(AllocationStatus.PENDING);
        return d;
    }

    private void mockPending(List<AllocationOrderDetail> pending) {
        when(aodRepo.findForUpdateByLocationCodeAndStatusOrderByAllocationNoAscItemNoAsc(
                LOCATION, AllocationStatus.PENDING)).thenReturn(pending);
    }

    private void mockLocation() {
        Location location = new Location();
        location.setLocationCode(LOCATION);
        location.setBranchCode(BRANCH);
        location.setLocationType(LocationType.CAR);
        when(locationRepo.findByLocationCode(LOCATION)).thenReturn(Optional.of(location));
    }

    @Test
    @DisplayName("無待領明細 - 應冪等返回，不取號不建單，也不必反查儲位")
    void receive_whenNothingPending_returnsEmptyWithoutCreating() {
        mockPending(List.of());

        List<SalesReceiveOrderDetailDto> result = service.receive(LOCATION);

        assertThat(result).isEmpty();
        verify(sequenceService, never()).generateSequence(any(), any());
        verify(sroRepo, never()).save(any());
        verifyNoInteractions(inventoryService, locationRepo);
    }

    @Test
    @DisplayName("儲位主檔查無資料 - 應拋 RESOURCE_NOT_FOUND，不得建單")
    void receive_whenLocationNotFound_throwsAndCreatesNothing() {
        mockPending(List.of(aod("AO-20260720-001", 1, "P001", "BATCH01", 10)));
        when(locationRepo.findByLocationCode(LOCATION)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.receive(LOCATION))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(sequenceService, never()).generateSequence(any(), any());
        verify(sroRepo, never()).save(any());
        verifyNoInteractions(inventoryService);
    }

    @Test
    @DisplayName("單一 AO - 應建 SRO/SROD、AOD 轉 RECEIVED、只加車存不動大庫")
    void receive_singleAllocationOrder_createsSroAndPicksUpToCarOnly() {
        AllocationOrderDetail d1 = aod("AO-20260720-001", 1, "P001", "BATCH01", 10);
        mockPending(List.of(d1));
        mockLocation();
        when(sequenceService.generateSequence(SequenceType.SRO, TODAY)).thenReturn(RECEIVE_NO);
        when(aodRepo.existsByAllocationNoAndStatus("AO-20260720-001", AllocationStatus.PENDING)).thenReturn(false);
        when(aoRepo.findByAllocationNo("AO-20260720-001")).thenReturn(Optional.of(ao("AO-20260720-001")));
        when(mapper.toDetailDtoList(anyList())).thenReturn(List.of());

        service.receive(LOCATION);

        ArgumentCaptor<SalesReceiveOrder> sroCaptor = ArgumentCaptor.forClass(SalesReceiveOrder.class);
        verify(sroRepo).save(sroCaptor.capture());
        SalesReceiveOrder sro = sroCaptor.getValue();
        assertThat(sro.getReceiveNo()).isEqualTo(RECEIVE_NO);
        assertThat(sro.getBranchCode()).isEqualTo(BRANCH);   // 由儲位主檔反查而來
        assertThat(sro.getLocationCode()).isEqualTo(LOCATION);
        assertThat(sro.getReceiveDate()).isEqualTo(TODAY);   // 取自注入的 Clock

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SalesReceiveOrderDetail>> srodCaptor = ArgumentCaptor.forClass(List.class);
        verify(srodRepo).saveAll(srodCaptor.capture());
        assertThat(srodCaptor.getValue()).singleElement().satisfies(srod -> {
            assertThat(srod.getReceiveNo()).isEqualTo(RECEIVE_NO);
            assertThat(srod.getItemNo()).isEqualTo(1);
            assertThat(srod.getAllocationNo()).isEqualTo("AO-20260720-001");
            assertThat(srod.getAllocationItemNo()).isEqualTo(1);
            assertThat(srod.getProductCode()).isEqualTo("P001");
            assertThat(srod.getBatchNo()).isEqualTo("BATCH01");
            assertThat(srod.getQty()).isEqualTo(10);   // 一次領完 = AOD.allocatedQty
        });

        assertThat(d1.getStatus()).isEqualTo(AllocationStatus.RECEIVED);

        // 只加車存；大庫已於配貨時預留扣除，領貨不得再動
        verify(inventoryService).pickUp(BRANCH, LOCATION, "P001", "BATCH01",
                LocalDate.of(2026, 12, 31), 10, RECEIVE_NO);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    @DisplayName("跨兩張 AO - SROD 的 itemNo 應連號且各自指回來源 AOD")
    void receive_acrossMultipleAllocationOrders_itemNoIsContinuousAndTracesSource() {
        AllocationOrderDetail d1 = aod("AO-20260719-001", 3, "P001", "BATCH01", 5);
        AllocationOrderDetail d2 = aod("AO-20260720-001", 1, "P002", "BATCH02", 8);
        mockPending(List.of(d1, d2));
        mockLocation();
        when(sequenceService.generateSequence(SequenceType.SRO, TODAY)).thenReturn(RECEIVE_NO);
        when(aodRepo.existsByAllocationNoAndStatus(any(), eq(AllocationStatus.PENDING))).thenReturn(true);
        when(mapper.toDetailDtoList(anyList())).thenReturn(List.of());

        service.receive(LOCATION);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SalesReceiveOrderDetail>> srodCaptor = ArgumentCaptor.forClass(List.class);
        verify(srodRepo).saveAll(srodCaptor.capture());
        List<SalesReceiveOrderDetail> srods = srodCaptor.getValue();

        assertThat(srods).extracting(SalesReceiveOrderDetail::getItemNo).containsExactly(1, 2);
        assertThat(srods).extracting(SalesReceiveOrderDetail::getAllocationNo)
                .containsExactly("AO-20260719-001", "AO-20260720-001");
        assertThat(srods).extracting(SalesReceiveOrderDetail::getAllocationItemNo).containsExactly(3, 1);
        assertThat(srods).extracting(SalesReceiveOrderDetail::getQty).containsExactly(5, 8);
    }

    @Test
    @DisplayName("AO 底下仍有他人未領明細 - AO 應維持 PENDING")
    void receive_whenOtherSalesStillPending_allocationOrderStaysPending() {
        mockPending(List.of(aod("AO-20260720-001", 1, "P001", "BATCH01", 10)));
        mockLocation();
        when(sequenceService.generateSequence(SequenceType.SRO, TODAY)).thenReturn(RECEIVE_NO);
        // 同一張 AO 底下還有別的業務員的 AOD 是 PENDING
        when(aodRepo.existsByAllocationNoAndStatus("AO-20260720-001", AllocationStatus.PENDING)).thenReturn(true);
        when(mapper.toDetailDtoList(anyList())).thenReturn(List.of());

        service.receive(LOCATION);

        verify(aoRepo, never()).findByAllocationNo(any());
        verify(aoRepo, never()).save(any());
    }

    @Test
    @DisplayName("AO 全部明細皆已領取 - AO 應轉 RECEIVED（聚合狀態）")
    void receive_whenAllDetailsReceived_allocationOrderTurnsReceived() {
        mockPending(List.of(aod("AO-20260720-001", 1, "P001", "BATCH01", 10)));
        mockLocation();
        when(sequenceService.generateSequence(SequenceType.SRO, TODAY)).thenReturn(RECEIVE_NO);
        when(aodRepo.existsByAllocationNoAndStatus("AO-20260720-001", AllocationStatus.PENDING)).thenReturn(false);
        AllocationOrder ao = ao("AO-20260720-001");
        when(aoRepo.findByAllocationNo("AO-20260720-001")).thenReturn(Optional.of(ao));
        when(mapper.toDetailDtoList(anyList())).thenReturn(List.of());

        service.receive(LOCATION);

        assertThat(ao.getStatus()).isEqualTo(AllocationStatus.RECEIVED);
        verify(aoRepo).save(ao);
    }
}
