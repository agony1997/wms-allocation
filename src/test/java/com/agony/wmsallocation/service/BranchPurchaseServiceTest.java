package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.purchase.AdjustConfirmedQtyRequest;
import com.agony.wmsallocation.dto.purchase.BranchPurchaseSummaryDto;
import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDto;
import com.agony.wmsallocation.entity.master.Product;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseFrozen;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrderDetail;
import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.exception.BusinessException;
import com.agony.wmsallocation.mapper.SalesPurchaseOrderMapper;
import com.agony.wmsallocation.repository.BranchPurchaseFrozenRepo;
import com.agony.wmsallocation.repository.ProductRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderDetailRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderRepo;
import com.agony.wmsallocation.security.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BranchPurchaseServiceTest {

    private static final String BRANCH = "B01";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 6);
    private static final String OPERATOR = "LEADER01";

    @Mock BranchPurchaseFrozenRepo bpfRepo;
    @Mock SalesPurchaseOrderRepo spoRepo;
    @Mock SalesPurchaseOrderDetailRepo spodRepo;
    @Mock ProductRepo productRepo;
    @Mock SalesPurchaseOrderMapper mapper;

    private BranchPurchaseService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-06T09:00:00Z"), ZoneOffset.UTC);
        service = new BranchPurchaseService(bpfRepo, spoRepo, spodRepo, productRepo, mapper, clock);
        // freeze/confirm 的操作者取自登入身份而非參數，模擬 JwtInterceptor 已寫入 ThreadLocal
        UserContextHolder.setUserCode(OPERATOR);
    }

    // ThreadLocal 不清會殘留到下一個重用此執行緒的測試
    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void getBranchSummary_whenNotFrozen_returnsNullStatus() {
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of());
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.empty());

        BranchPurchaseSummaryDto dto = service.getBranchSummary(BRANCH, DATE);

        Assertions.assertNull(dto.getFrozenStatus());
        Assertions.assertTrue(dto.getOrders().isEmpty());
    }

    @Test
    void getBranchSummary_whenFrozen_includesOrderDetails() {
        SalesPurchaseOrder spo = spoOf("SPO-001", "L01");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo));
        Mockito.when(mapper.toDto(spo)).thenReturn(SalesPurchaseOrderDto.builder().build());
        Mockito.when(spodRepo.findByPurchaseNoOrderBySortOrder("SPO-001")).thenReturn(List.of());
        Mockito.when(mapper.toDetailDtoList(List.of())).thenReturn(List.of());

        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        BranchPurchaseSummaryDto dto = service.getBranchSummary(BRANCH, DATE);

        Assertions.assertEquals(FrozenStatus.FROZEN, dto.getFrozenStatus());
        Assertions.assertEquals(1, dto.getOrders().size());
    }

    @Test
    void freeze_whenAlreadyFrozen_throws() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE))
                .thenReturn(Optional.of(new BranchPurchaseFrozen()));

        Assertions.assertThrows(BusinessException.class, () -> service.freeze(BRANCH, DATE));

        Mockito.verify(bpfRepo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void freeze_whenOpen_createsFrozenRecord() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.empty());

        service.freeze(BRANCH, DATE);

        ArgumentCaptor<BranchPurchaseFrozen> captor = ArgumentCaptor.forClass(BranchPurchaseFrozen.class);
        Mockito.verify(bpfRepo).save(captor.capture());
        Assertions.assertEquals(FrozenStatus.FROZEN, captor.getValue().getStatus());
        Assertions.assertEquals(OPERATOR, captor.getValue().getFrozenBy());
    }

    @Test
    void unfreeze_whenNotFrozen_throws() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> service.unfreeze(BRANCH, DATE));
    }

    @Test
    void unfreeze_whenConfirmed_throwsAndDoesNotDelete() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.CONFIRMED);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        Assertions.assertThrows(BusinessException.class, () -> service.unfreeze(BRANCH, DATE));

        Mockito.verify(bpfRepo, Mockito.never()).delete(Mockito.any());
    }

    @Test
    void unfreeze_whenFrozen_deletesRecord() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        service.unfreeze(BRANCH, DATE);

        Mockito.verify(bpfRepo).delete(bpf);
    }

    @Test
    void confirm_whenNotFrozen_throws() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> service.confirm(BRANCH, DATE));
    }

    @Test
    void confirm_whenAlreadyConfirmed_isIdempotentAndDoesNotSave() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.CONFIRMED);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        service.confirm(BRANCH, DATE);

        Mockito.verify(bpfRepo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void confirm_whenFrozen_setsConfirmed() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        service.confirm(BRANCH, DATE);

        Mockito.verify(bpfRepo).save(bpf);
        Assertions.assertEquals(FrozenStatus.CONFIRMED, bpf.getStatus());
        Assertions.assertEquals(OPERATOR, bpf.getConfirmedBy());
    }

    @Test
    void adjustConfirmedQty_whenNotFrozen_throws() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class,
                () -> service.adjustConfirmedQty(BRANCH, DATE, requestOf("L01", "P001", "箱", 10)));
    }

    @Test
    void adjustConfirmedQty_whenConfirmed_throws() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.CONFIRMED);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        Assertions.assertThrows(BusinessException.class,
                () -> service.adjustConfirmedQty(BRANCH, DATE, requestOf("L01", "P001", "箱", 10)));
    }

    // 既有明細找得到同商品同單位 → 就地更新 confirmedQty，不新增
    @Test
    void adjustConfirmedQty_whenDetailExists_updatesInPlace() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        SalesPurchaseOrder spo = spoOf("SPO-001", "L01");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo));

        SalesPurchaseOrderDetail existing = detailOf("SPO-001", 1, "P001", "箱", 5);
        Mockito.when(spodRepo.findByPurchaseNoOrderBySortOrder("SPO-001")).thenReturn(List.of(existing));

        service.adjustConfirmedQty(BRANCH, DATE, requestOf("L01", "P001", "箱", 10));

        Mockito.verify(spodRepo).save(existing);
        Assertions.assertEquals(10, existing.getConfirmedQty());
        Mockito.verifyNoInteractions(productRepo);
    }

    // 找不到同商品同單位的既有明細 → 視為組長新增品項，qty 預設 0、品名補自 Product 主檔
    @Test
    void adjustConfirmedQty_whenDetailMissing_createsNewLine() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        SalesPurchaseOrder spo = spoOf("SPO-001", "L01");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo));

        SalesPurchaseOrderDetail existing = detailOf("SPO-001", 1, "P001", "箱", 5);
        Mockito.when(spodRepo.findByPurchaseNoOrderBySortOrder("SPO-001")).thenReturn(List.of(existing));

        Product product = new Product();
        product.setProductCode("P002");
        product.setProductName("新商品");
        Mockito.when(productRepo.findByProductCode("P002")).thenReturn(Optional.of(product));

        service.adjustConfirmedQty(BRANCH, DATE, requestOf("L01", "P002", "個", 20));

        ArgumentCaptor<SalesPurchaseOrderDetail> captor = ArgumentCaptor.forClass(SalesPurchaseOrderDetail.class);
        Mockito.verify(spodRepo).save(captor.capture());
        SalesPurchaseOrderDetail created = captor.getValue();
        Assertions.assertEquals("P002", created.getProductCode());
        Assertions.assertEquals("新商品", created.getProductName());
        Assertions.assertEquals("個", created.getUnit());
        Assertions.assertEquals(0, created.getQty());
        Assertions.assertEquals(20, created.getConfirmedQty());
        Assertions.assertEquals(2, created.getItemNo());   // 既有明細 itemNo=1，新增接續為 2
        Assertions.assertEquals(SalesOrderDetailStatus.PENDING, created.getStatus());
    }

    @Test
    void adjustConfirmedQty_whenNewProductNotInMaster_throws() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        SalesPurchaseOrder spo = spoOf("SPO-001", "L01");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo));
        Mockito.when(spodRepo.findByPurchaseNoOrderBySortOrder("SPO-001")).thenReturn(List.of());
        Mockito.when(productRepo.findByProductCode("P999")).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class,
                () -> service.adjustConfirmedQty(BRANCH, DATE, requestOf("L01", "P999", "箱", 10)));

        Mockito.verify(spodRepo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void adjustConfirmedQty_whenLocationCodeUnknown_skipsSilently() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of());

        service.adjustConfirmedQty(BRANCH, DATE, requestOf("L99", "P001", "箱", 10));

        Mockito.verifyNoInteractions(spodRepo);
    }

    private SalesPurchaseOrder spoOf(String purchaseNo, String locationCode) {
        SalesPurchaseOrder spo = new SalesPurchaseOrder();
        spo.setPurchaseNo(purchaseNo);
        spo.setLocationCode(locationCode);
        return spo;
    }

    private SalesPurchaseOrderDetail detailOf(String purchaseNo, int itemNo, String productCode, String unit, int confirmedQty) {
        SalesPurchaseOrderDetail d = new SalesPurchaseOrderDetail();
        d.setPurchaseNo(purchaseNo);
        d.setItemNo(itemNo);
        d.setProductCode(productCode);
        d.setUnit(unit);
        d.setConfirmedQty(confirmedQty);
        return d;
    }

    private AdjustConfirmedQtyRequest requestOf(String locationCode, String productCode, String unit, int confirmedQty) {
        AdjustConfirmedQtyRequest request = new AdjustConfirmedQtyRequest();
        request.setAdjustments(List.of(new AdjustConfirmedQtyRequest.Detail(locationCode, productCode, unit, confirmedQty)));
        return request;
    }
}
