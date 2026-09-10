package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDetailDto;
import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDto;
import com.agony.wmsallocation.dto.purchase.SavePurchaseRequest;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseFrozen;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrderDetail;
import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.entity.branch.Location;
import com.agony.wmsallocation.exception.BusinessException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.SalesPurchaseOrderMapper;
import com.agony.wmsallocation.repository.BranchPurchaseFrozenRepo;
import com.agony.wmsallocation.repository.LocationRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderDetailRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderRepo;
import com.agony.wmsallocation.security.DataScopeGuard;
import com.agony.wmsallocation.security.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class SalesPurchaseServiceTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 2);   // 固定今天，合法區間 = 07-04 ~ 07-11

    @Mock SalesPurchaseOrderRepo spoRepo;
    @Mock SalesPurchaseOrderDetailRepo spodRepo;
    @Mock BranchPurchaseFrozenRepo bpfRepo;
    @Mock SequenceService sequenceService;
    @Mock SalesPurchaseOrderMapper mapper;
    @Mock LocationRepo locationRepo;

    private SalesPurchaseService service;

    @BeforeEach
    void setUp() {
        Clock fixed = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);
        service = new SalesPurchaseService(spoRepo, spodRepo, bpfRepo, sequenceService, mapper, fixed,
                new DataScopeGuard(locationRepo));
        // 預設操作者在 B01 具備 LEADER，讓既有案例不受新加的資料範圍檢查影響
        UserContextHolder.setBranchRoles(Map.of("B01", Set.of("LEADER")));
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    // 合法訂貨日為 D+2 ~ D+9；區間外一律拒。此處只釘拒絕側，
    // 合法邊界（D+2、D+9）的成功路徑由下方 happy path 涵蓋。
    @ParameterizedTest
    @ValueSource(strings = {
            "2026-07-01",   // 過去
            "2026-07-02",   // 今天 (D+0)
            "2026-07-03",   // D+1，未達下限
            "2026-07-12",   // D+10，超過上限
            "2026-08-01",   // 遠期
    })
    void save_whenDateOutOfRange_throwsBusinessException(String date) {
        SavePurchaseRequest request = requestOn(LocalDate.parse(date));
        Assertions.assertThrows(BusinessException.class, () -> service.save(request));
    }

    // happy path：無既有單 → 取號建單、複製 qty 到 confirmedQty、明細狀態 PENDING、整批覆蓋
    @Test
    void save_whenNoExistingOrder_createsOrderAndCopiesQtyToConfirmed() {
        LocalDate date = TODAY.plusDays(2);   // 07-04，合法

        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date))
                .thenReturn(Optional.empty());

        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.empty());

        Mockito.when(sequenceService.generateSequence(SequenceType.SPO, date))
                .thenReturn("SPO-20260704-001");

        Mockito.when(spoRepo.save(Mockito.any(SalesPurchaseOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(mapper.toDto(Mockito.any(SalesPurchaseOrder.class)))
                .thenReturn(SalesPurchaseOrderDto.builder().build());

        SavePurchaseRequest request = new SavePurchaseRequest("B01", "L01", date, "U01",
                List.of(new SavePurchaseRequest.Detail("P001", "箱", 10, 1)));

        service.save(request);

        ArgumentCaptor<SalesPurchaseOrder> spoCaptor = ArgumentCaptor.forClass(SalesPurchaseOrder.class);
        Mockito.verify(spoRepo).save(spoCaptor.capture());

        SalesPurchaseOrder saved = spoCaptor.getValue();
        Assertions.assertEquals("SPO-20260704-001", saved.getPurchaseNo());
        Assertions.assertEquals("B01", saved.getBranchCode());
        Assertions.assertEquals("L01", saved.getLocationCode());
        Assertions.assertEquals(date, saved.getPurchaseDate());
        Assertions.assertEquals("U01", saved.getPurchaseUser());   // 單主＝業務員，非操作者

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SalesPurchaseOrderDetail>> detailCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(spodRepo).saveAll(detailCaptor.capture());

        SalesPurchaseOrderDetail d = detailCaptor.getValue().get(0);
        Assertions.assertEquals("P001", d.getProductCode());
        Assertions.assertEquals(10, d.getQty());
        Assertions.assertEquals(10, d.getConfirmedQty());   // 建立時複製 qty
        Assertions.assertEquals(SalesOrderDetailStatus.PENDING, d.getStatus());

        Mockito.verify(spodRepo).deleteByPurchaseNo("SPO-20260704-001");   // 整批覆蓋：先清舊明細
    }

    // 凍結守門：BPF 已 FROZEN → 業務員不可儲存，且不得寫入任何資料
    @Test
    void save_whenBranchFrozen_throwsAndDoesNotWrite() {
        LocalDate date = TODAY.plusDays(2);
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date))
                .thenReturn(Optional.of(bpf));

        SavePurchaseRequest request = new SavePurchaseRequest("B01", "L01", date, "U01", List.of());

        Assertions.assertThrows(BusinessException.class, () -> service.save(request));

        Mockito.verify(spoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(spodRepo, Mockito.never()).saveAll(Mockito.any());
    }

    // find：無單 → 回空白表單，details 為空 list（非 null），BPF 不存在故可編輯
    @Test
    void find_whenNoOrder_returnsBlankEditableForm() {
        LocalDate date = TODAY.plusDays(2);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date)).thenReturn(Optional.empty());
        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.empty());

        SalesPurchaseOrderDto dto = service.find("B01", "L01", date);

        Assertions.assertNull(dto.getPurchaseNo());          // 尚未建立
        Assertions.assertTrue(dto.isEditable());             // BPF 不存在 = 可編輯
        Assertions.assertNotNull(dto.getDetails());          // 空 list，不可為 null
        Assertions.assertTrue(dto.getDetails().isEmpty());
    }

    // find：有單 → 帶出明細（mapper.toDetailDtoList），BPF 不存在故可編輯
    @Test
    void find_whenOrderExists_loadsDetails() {
        LocalDate date = TODAY.plusDays(2);
        SalesPurchaseOrder spo = new SalesPurchaseOrder();
        spo.setPurchaseNo("SPO-20260704-001");
        SalesPurchaseOrderDetail detail = new SalesPurchaseOrderDetail();

        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date)).thenReturn(Optional.empty());
        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.of(spo));
        Mockito.when(mapper.toDto(spo))
                .thenReturn(SalesPurchaseOrderDto.builder().purchaseNo("SPO-20260704-001").build());
        Mockito.when(spodRepo.findByPurchaseNoOrderBySortOrder("SPO-20260704-001"))
                .thenReturn(List.of(detail));
        Mockito.when(mapper.toDetailDtoList(List.of(detail)))
                .thenReturn(List.of(SalesPurchaseOrderDetailDto.builder().productCode("P001").build()));

        SalesPurchaseOrderDto dto = service.find("B01", "L01", date);

        Assertions.assertTrue(dto.isEditable());
        Assertions.assertEquals(1, dto.getDetails().size());
        Assertions.assertEquals("P001", dto.getDetails().get(0).getProductCode());
    }

    // find：BPF 已 FROZEN → 業務員不可編輯（直接釘住 editable 的方向，不能貼反）
    @Test
    void find_whenFrozen_notEditable() {
        LocalDate date = TODAY.plusDays(2);
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date)).thenReturn(Optional.of(bpf));
        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.empty());

        SalesPurchaseOrderDto dto = service.find("B01", "L01", date);

        Assertions.assertFalse(dto.isEditable());
    }

    // ── 資料範圍授權：LEADER 走 assertBranchAccess，SALES 走 assertLocationOwnership ──

    // 該所 LEADER 代業務員下單：已由上方所有 happy path 測試（@BeforeEach 預設 LEADER）覆蓋，
    // 此處單獨補一支具名案例，對應規格「LEADER 可代任一儲位下單」

    @Test
    void save_whenLeaderOfBranch_isAllowed() {
        LocalDate date = TODAY.plusDays(2);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date)).thenReturn(Optional.empty());
        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.empty());
        Mockito.when(sequenceService.generateSequence(SequenceType.SPO, date)).thenReturn("SPO-20260704-001");
        Mockito.when(spoRepo.save(Mockito.any(SalesPurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(mapper.toDto(Mockito.any(SalesPurchaseOrder.class))).thenReturn(SalesPurchaseOrderDto.builder().build());

        Assertions.assertDoesNotThrow(() -> service.save(requestOn(date)));
    }

    @Test
    void save_whenSalesOwnsLocation_isAllowed() {
        UserContextHolder.setBranchRoles(Map.of());   // 無 LEADER，走 SALES 儲位分流
        UserContextHolder.setUserCode("SALES01");
        Location location = new Location();
        location.setLocationCode("L01");
        location.setUserCode("SALES01");
        Mockito.when(locationRepo.findByLocationCode("L01")).thenReturn(Optional.of(location));

        LocalDate date = TODAY.plusDays(2);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date)).thenReturn(Optional.empty());
        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.empty());
        Mockito.when(sequenceService.generateSequence(SequenceType.SPO, date)).thenReturn("SPO-20260704-001");
        Mockito.when(spoRepo.save(Mockito.any(SalesPurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(mapper.toDto(Mockito.any(SalesPurchaseOrder.class))).thenReturn(SalesPurchaseOrderDto.builder().build());

        Assertions.assertDoesNotThrow(() -> service.save(requestOn(date)));
    }

    @Test
    void save_whenSalesTargetsOthersLocation_throwsAccessDenied() {
        UserContextHolder.setBranchRoles(Map.of());   // 無 LEADER，走 SALES 儲位分流
        UserContextHolder.setUserCode("SALES01");
        Location location = new Location();
        location.setLocationCode("L01");
        location.setUserCode("OTHER_USER");
        Mockito.when(locationRepo.findByLocationCode("L01")).thenReturn(Optional.of(location));

        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> service.save(requestOn(TODAY.plusDays(2))));

        Assertions.assertEquals(ErrorCode.LOCATION_ACCESS_DENIED, ex.getErrorCode());
        Mockito.verifyNoInteractions(bpfRepo, spoRepo, spodRepo);
    }

    @Test
    void save_whenAdminOfOtherBranch_isAllowed() {
        UserContextHolder.setBranchRoles(Map.of("OTHER", Set.of("ADMIN")));   // 非該所 LEADER，走 SALES 分流；ADMIN 在 guard 內直接放行

        LocalDate date = TODAY.plusDays(2);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate("B01", date)).thenReturn(Optional.empty());
        Mockito.when(spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate("B01", "L01", date))
                .thenReturn(Optional.empty());
        Mockito.when(sequenceService.generateSequence(SequenceType.SPO, date)).thenReturn("SPO-20260704-001");
        Mockito.when(spoRepo.save(Mockito.any(SalesPurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(mapper.toDto(Mockito.any(SalesPurchaseOrder.class))).thenReturn(SalesPurchaseOrderDto.builder().build());

        // ADMIN 一律放行，assertLocationOwnership 內部略過檢查，不需 mock locationRepo
        Assertions.assertDoesNotThrow(() -> service.save(requestOn(date)));
        Mockito.verifyNoInteractions(locationRepo);
    }

    private SavePurchaseRequest requestOn(LocalDate purchaseDate) {
        return new SavePurchaseRequest("B01", "L01", purchaseDate, "U01", List.of());
    }
}
