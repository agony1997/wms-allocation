package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.purchase.BranchPurchaseOrderDto;
import com.agony.wmsallocation.entity.master.Product;
import com.agony.wmsallocation.entity.master.ProductFactory;
import com.agony.wmsallocation.entity.purchase.*;
import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessException;
import com.agony.wmsallocation.mapper.BranchPurchaseOrderMapper;
import com.agony.wmsallocation.repository.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class BranchPurchaseOrderServiceTest {

    private static final String BRANCH = "B01";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 4);

    @Mock BranchPurchaseFrozenRepo bpfRepo;
    @Mock SalesPurchaseOrderRepo spoRepo;
    @Mock SalesPurchaseOrderDetailRepo spodRepo;
    @Mock ProductFactoryRepo productFactoryRepo;
    @Mock ProductRepo productRepo;
    @Mock BranchPurchaseOrderRepo bpoRepo;
    @Mock BranchPurchaseOrderDetailRepo bpodRepo;
    @Mock SequenceService sequenceService;
    @Mock BranchPurchaseOrderMapper mapper;

    private BranchPurchaseOrderService service;

    @BeforeEach
    void setUp() {
        service = new BranchPurchaseOrderService(bpfRepo, spoRepo, spodRepo, productFactoryRepo, productRepo,
                bpoRepo, bpodRepo, sequenceService, mapper);
    }

    @Test
    void aggregate_whenBpfNotExists_throwsAndDoesNotWrite() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> service.aggregate(BRANCH, DATE));

        Mockito.verify(bpoRepo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void aggregate_whenBpfNotConfirmed_throwsAndDoesNotWrite() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.FROZEN);
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(bpf));

        Assertions.assertThrows(BusinessException.class, () -> service.aggregate(BRANCH, DATE));

        Mockito.verify(bpoRepo, Mockito.never()).save(Mockito.any());
    }

    // 已彙總過或當天無訂單 → 冪等回空 list，不寫入
    @Test
    void aggregate_whenNoPendingDetail_returnsEmptyAndDoesNotWrite() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(confirmedBpf()));

        SalesPurchaseOrder spo = spoOf("SPO-20260704-001");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo));
        Mockito.when(spodRepo.findByPurchaseNoInAndStatus(List.of("SPO-20260704-001"), SalesOrderDetailStatus.PENDING))
                .thenReturn(List.of());

        List<BranchPurchaseOrderDto> result = service.aggregate(BRANCH, DATE);

        Assertions.assertTrue(result.isEmpty());
        Mockito.verify(bpoRepo, Mockito.never()).save(Mockito.any());
    }

    // happy path：兩筆不同 SPO 的同商品同工廠同單位 SPOD → 併成一筆 BPOD，qty = confirmedQty 加總，明細轉 AGGREGATED
    @Test
    void aggregate_mergesSameProductAcrossOrders_andMarksAggregated() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(confirmedBpf()));

        SalesPurchaseOrder spo1 = spoOf("SPO-20260704-001");
        SalesPurchaseOrder spo2 = spoOf("SPO-20260704-002");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo1, spo2));

        SalesPurchaseOrderDetail d1 = detailOf("SPO-20260704-001", "P001", "箱", 5);
        SalesPurchaseOrderDetail d2 = detailOf("SPO-20260704-002", "P001", "箱", 3);
        Mockito.when(spodRepo.findByPurchaseNoInAndStatus(
                        List.of("SPO-20260704-001", "SPO-20260704-002"), SalesOrderDetailStatus.PENDING))
                .thenReturn(List.of(d1, d2));

        ProductFactory pf = new ProductFactory();
        pf.setProductCode("P001");
        pf.setFactoryCode("F01");
        pf.setIsDefault(true);
        Mockito.when(productFactoryRepo.findByProductCodeInAndIsDefaultTrue(Mockito.anyList())).thenReturn(List.of(pf));

        Product product = new Product();
        product.setProductCode("P001");
        product.setProductName("商品A");
        Mockito.when(productRepo.findByProductCodeIn(Mockito.anyList())).thenReturn(List.of(product));

        Mockito.when(sequenceService.generateSequence(SequenceType.BPO, DATE)).thenReturn("BPO-20260704-001");
        Mockito.when(bpoRepo.save(Mockito.any(BranchPurchaseOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(mapper.toDto(Mockito.any(BranchPurchaseOrder.class))).thenReturn(BranchPurchaseOrderDto.builder().build());
        Mockito.when(mapper.toDetailDtoList(Mockito.anyList())).thenReturn(List.of());

        List<BranchPurchaseOrderDto> result = service.aggregate(BRANCH, DATE);

        Assertions.assertEquals(1, result.size());

        ArgumentCaptor<BranchPurchaseOrder> bpoCaptor = ArgumentCaptor.forClass(BranchPurchaseOrder.class);
        Mockito.verify(bpoRepo).save(bpoCaptor.capture());
        Assertions.assertEquals("BPO-20260704-001", bpoCaptor.getValue().getBpoNo());
        Assertions.assertEquals("F01", bpoCaptor.getValue().getFactoryCode());
        Assertions.assertEquals(BpoStatus.PENDING, bpoCaptor.getValue().getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BranchPurchaseOrderDetail>> bpodCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(bpodRepo).saveAll(bpodCaptor.capture());
        Assertions.assertEquals(1, bpodCaptor.getValue().size());
        Assertions.assertEquals(8, bpodCaptor.getValue().get(0).getQty());   // 5 + 3，加總的是 confirmedQty
        Assertions.assertEquals("商品A", bpodCaptor.getValue().get(0).getProductName());   // 來自 ProductRepo，非 SPOD

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SalesPurchaseOrderDetail>> spodCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(spodRepo).saveAll(spodCaptor.capture());
        Assertions.assertTrue(spodCaptor.getValue().stream()
                .allMatch(d -> d.getStatus() == SalesOrderDetailStatus.AGGREGATED
                        && "BPO-20260704-001".equals(d.getBpoNo())));   // 彙總同時回填來源 BPO
    }

    // 商品缺 ProductFactory.isDefault 對應 → 丟例外，且未建立任何 BPO（驗證先驗證完再動手，不留部分寫入）
    @Test
    void aggregate_whenProductFactoryMissing_throwsAndCreatesNoBpo() {
        Mockito.when(bpfRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(Optional.of(confirmedBpf()));

        SalesPurchaseOrder spo = spoOf("SPO-20260704-001");
        Mockito.when(spoRepo.findByBranchCodeAndPurchaseDate(BRANCH, DATE)).thenReturn(List.of(spo));

        SalesPurchaseOrderDetail d1 = detailOf("SPO-20260704-001", "P001", "箱", 5);
        Mockito.when(spodRepo.findByPurchaseNoInAndStatus(Mockito.anyList(), Mockito.eq(SalesOrderDetailStatus.PENDING)))
                .thenReturn(List.of(d1));

        Mockito.when(productFactoryRepo.findByProductCodeInAndIsDefaultTrue(Mockito.anyList())).thenReturn(List.of());

        Assertions.assertThrows(BusinessException.class, () -> service.aggregate(BRANCH, DATE));

        Mockito.verify(bpoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(sequenceService, Mockito.never()).generateSequence(Mockito.any(), Mockito.any());
    }

    private BranchPurchaseFrozen confirmedBpf() {
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setStatus(FrozenStatus.CONFIRMED);
        return bpf;
    }

    private SalesPurchaseOrder spoOf(String purchaseNo) {
        SalesPurchaseOrder spo = new SalesPurchaseOrder();
        spo.setPurchaseNo(purchaseNo);
        return spo;
    }

    private SalesPurchaseOrderDetail detailOf(String purchaseNo, String productCode, String unit, int confirmedQty) {
        SalesPurchaseOrderDetail d = new SalesPurchaseOrderDetail();
        d.setPurchaseNo(purchaseNo);
        d.setProductCode(productCode);
        d.setUnit(unit);
        d.setConfirmedQty(confirmedQty);
        d.setStatus(SalesOrderDetailStatus.PENDING);
        return d;
    }
}
