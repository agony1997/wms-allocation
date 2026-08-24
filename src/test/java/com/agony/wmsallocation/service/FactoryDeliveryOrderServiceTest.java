package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.receive.FactoryDeliveryOrderDto;
import com.agony.wmsallocation.dto.receive.ReceiveFactoryDeliveryOrderRequest;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.BranchPurchaseOrderDetail;
import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import com.agony.wmsallocation.entity.receive.FactoryDeliveryOrder;
import com.agony.wmsallocation.entity.receive.FactoryDeliveryOrderDetail;
import com.agony.wmsallocation.entity.receive.enums.FactoryDeliveryStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.FactoryDeliveryOrderMapper;
import com.agony.wmsallocation.repository.BranchPurchaseOrderDetailRepo;
import com.agony.wmsallocation.repository.BranchPurchaseOrderRepo;
import com.agony.wmsallocation.repository.FactoryDeliveryOrderDetailRepo;
import com.agony.wmsallocation.repository.FactoryDeliveryOrderRepo;
import com.agony.wmsallocation.security.UserContextHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class FactoryDeliveryOrderServiceTest {

    private static final String BPO_NO = "BPO-20260704-001";
    private static final String BRANCH = "B01";
    private static final String FACTORY = "F01";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 4);
    private static final String FDO_NO = "FDO-20260704-001";
    private static final String BATCH_NO = "F01-20260704";
    private static final LocalDate EXPIRY = LocalDate.of(2027, 1, 4);
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 7, 8, 10, 0, 0);

    @Mock BranchPurchaseOrderRepo bpoRepo;
    @Mock BranchPurchaseOrderDetailRepo bpodRepo;
    @Mock FactoryDeliveryOrderRepo fdoRepo;
    @Mock FactoryDeliveryOrderDetailRepo fdodRepo;
    @Mock SequenceService sequenceService;
    @Mock FactoryDeliveryOrderMapper mapper;
    @Mock InventoryService inventoryService;

    private FactoryDeliveryOrderService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-08T10:00:00Z"), ZoneOffset.UTC);
        service = new FactoryDeliveryOrderService(bpoRepo, bpodRepo, fdoRepo, fdodRepo, sequenceService, mapper, inventoryService, clock);
    }

    @Test
    void ship_whenBpoNotFound_throwsAndDoesNotWrite() {
        Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.empty());

        Assertions.assertThrows(BusinessException.class, () -> service.ship(BPO_NO));

        Mockito.verify(fdoRepo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void ship_whenAlreadyShipped_throwsAndDoesNotWrite() {
        Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.of(bpo()));
        Mockito.when(fdoRepo.existsByBpoNo(BPO_NO)).thenReturn(true);

        Assertions.assertThrows(BusinessException.class, () -> service.ship(BPO_NO));

        Mockito.verify(fdoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(sequenceService, Mockito.never()).generateSequence(Mockito.any(), Mockito.any());
    }

    @Test
    void ship_whenBpoHasNoDetail_throwsAndDoesNotWrite() {
        Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.of(bpo()));
        Mockito.when(fdoRepo.existsByBpoNo(BPO_NO)).thenReturn(false);
        Mockito.when(bpodRepo.findByBpoNoOrderByItemNo(BPO_NO)).thenReturn(List.of());

        Assertions.assertThrows(BusinessException.class, () -> service.ship(BPO_NO));

        Mockito.verify(fdoRepo, Mockito.never()).save(Mockito.any());
    }

    // happy path：BPOD 逐項搬成 FDOD，qty 全額帶入、狀態 PENDING、批次/效期自動生成、receivedQty 留空
    @Test
    void ship_copiesDetailsAndGeneratesBatch() {
        Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.of(bpo()));
        Mockito.when(fdoRepo.existsByBpoNo(BPO_NO)).thenReturn(false);
        Mockito.when(bpodRepo.findByBpoNoOrderByItemNo(BPO_NO))
                .thenReturn(List.of(bpod(1, "P001", "商品A", "箱", 8)));
        Mockito.when(sequenceService.generateSequence(SequenceType.FDO, DATE)).thenReturn("FDO-20260704-001");
        Mockito.when(fdoRepo.save(Mockito.any(FactoryDeliveryOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(mapper.toDto(Mockito.any(FactoryDeliveryOrder.class))).thenReturn(FactoryDeliveryOrderDto.builder().build());
        Mockito.when(mapper.toDetailDtoList(Mockito.anyList())).thenReturn(List.of());

        service.ship(BPO_NO);

        ArgumentCaptor<FactoryDeliveryOrder> fdoCaptor = ArgumentCaptor.forClass(FactoryDeliveryOrder.class);
        Mockito.verify(fdoRepo).save(fdoCaptor.capture());
        FactoryDeliveryOrder fdo = fdoCaptor.getValue();
        Assertions.assertEquals("FDO-20260704-001", fdo.getFdoNo());
        Assertions.assertEquals(BPO_NO, fdo.getBpoNo());
        Assertions.assertEquals(BRANCH, fdo.getBranchCode());
        Assertions.assertEquals(FACTORY, fdo.getFactoryCode());
        Assertions.assertEquals(DATE, fdo.getDeliveryDate());
        Assertions.assertEquals(FactoryDeliveryStatus.PENDING, fdo.getStatus());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FactoryDeliveryOrderDetail>> detailCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(fdodRepo).saveAll(detailCaptor.capture());
        List<FactoryDeliveryOrderDetail> details = detailCaptor.getValue();
        Assertions.assertEquals(1, details.size());
        FactoryDeliveryOrderDetail d = details.get(0);
        Assertions.assertEquals("FDO-20260704-001", d.getFdoNo());
        Assertions.assertEquals("P001", d.getProductCode());
        Assertions.assertEquals("商品A", d.getProductName());
        Assertions.assertEquals(8, d.getQty());                       // 全額出貨 = BPOD.qty
        Assertions.assertNull(d.getReceivedQty());                    // 收貨階段才填
        Assertions.assertEquals("F01-20260704", d.getBatchNo());      // 工廠 + 出貨日
        Assertions.assertEquals(LocalDate.of(2027, 1, 4), d.getExpiryDate());   // 出貨日 + 半年
    }

    @Test
    void receive_whenFdoNotFound_throwsAndDoesNotWrite() {
        Mockito.when(fdoRepo.findByFdoNo(FDO_NO)).thenReturn(Optional.empty());

        ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8));

        Assertions.assertThrows(BusinessException.class, () -> service.receive(request));

        Mockito.verify(fdoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(fdodRepo, Mockito.never()).saveAll(Mockito.any());
        Mockito.verify(bpoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(inventoryService, Mockito.never())
                .receive(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void receive_whenStatusNotPending_throwsAndDoesNotWrite() {
        Mockito.when(fdoRepo.findByFdoNo(FDO_NO)).thenReturn(Optional.of(fdo(FactoryDeliveryStatus.RECEIVED)));

        ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8));

        Assertions.assertThrows(BusinessException.class, () -> service.receive(request));

        Mockito.verify(fdoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(inventoryService, Mockito.never())
                .receive(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyInt(), Mockito.any());
    }

    @Test
    void receive_whenItemNoMissingFromRequest_throwsAndDoesNotWrite() {
        Mockito.when(fdoRepo.findByFdoNo(FDO_NO)).thenReturn(Optional.of(fdo(FactoryDeliveryStatus.PENDING)));
        Mockito.when(fdodRepo.findByFdoNoOrderByItemNo(FDO_NO)).thenReturn(List.of(
                fdod(1, "P001", 8),
                fdod(2, "P002", 5)));

        // 只帶 itemNo=1，漏了 itemNo=2
        ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8));

        Assertions.assertThrows(BusinessException.class, () -> service.receive(request));

        Mockito.verify(fdodRepo, Mockito.never()).saveAll(Mockito.any());
        Mockito.verify(fdoRepo, Mockito.never()).save(Mockito.any());
        Mockito.verify(bpoRepo, Mockito.never()).save(Mockito.any());
    }

    @Test
    void receive_whenAllQtyMatch_setsReceivedAndSyncsUpstream() {
        try {
            UserContextHolder.setUserCode("U001");

            Mockito.when(fdoRepo.findByFdoNo(FDO_NO)).thenReturn(Optional.of(fdo(FactoryDeliveryStatus.PENDING)));
            Mockito.when(fdodRepo.findByFdoNoOrderByItemNo(FDO_NO)).thenReturn(List.of(
                    fdod(1, "P001", 8),
                    fdod(2, "P002", 5)));
            Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.of(bpo()));
            Mockito.when(fdoRepo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
            Mockito.when(mapper.toDto(Mockito.any())).thenReturn(FactoryDeliveryOrderDto.builder().build());
            Mockito.when(mapper.toDetailDtoList(Mockito.anyList())).thenReturn(List.of());

            ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                    new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8),
                    new ReceiveFactoryDeliveryOrderRequest.Detail(2, 5));

            service.receive(request);

            ArgumentCaptor<FactoryDeliveryOrder> fdoCaptor = ArgumentCaptor.forClass(FactoryDeliveryOrder.class);
            Mockito.verify(fdoRepo).save(fdoCaptor.capture());
            FactoryDeliveryOrder savedFdo = fdoCaptor.getValue();
            Assertions.assertEquals(FactoryDeliveryStatus.RECEIVED, savedFdo.getStatus());
            Assertions.assertEquals(FIXED_NOW, savedFdo.getReceivedAt());
            Assertions.assertEquals("U001", savedFdo.getReceivedBy());

            ArgumentCaptor<BranchPurchaseOrder> bpoCaptor = ArgumentCaptor.forClass(BranchPurchaseOrder.class);
            Mockito.verify(bpoRepo).save(bpoCaptor.capture());
            Assertions.assertEquals(BpoStatus.RECEIVED, bpoCaptor.getValue().getStatus());

            Mockito.verify(inventoryService).receive(BRANCH, "P001", BATCH_NO, EXPIRY, 8, FDO_NO);
            Mockito.verify(inventoryService).receive(BRANCH, "P002", BATCH_NO, EXPIRY, 5, FDO_NO);
        } finally {
            UserContextHolder.clear();
        }
    }

    @Test
    void receive_whenAnyQtyMismatch_setsDiscrepancyAndSyncsUpstream() {
        try {
            UserContextHolder.setUserCode("U001");

            Mockito.when(fdoRepo.findByFdoNo(FDO_NO)).thenReturn(Optional.of(fdo(FactoryDeliveryStatus.PENDING)));
            Mockito.when(fdodRepo.findByFdoNoOrderByItemNo(FDO_NO)).thenReturn(List.of(
                    fdod(1, "P001", 8),
                    fdod(2, "P002", 5)));
            Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.of(bpo()));
            Mockito.when(fdoRepo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
            Mockito.when(mapper.toDto(Mockito.any())).thenReturn(FactoryDeliveryOrderDto.builder().build());
            Mockito.when(mapper.toDetailDtoList(Mockito.anyList())).thenReturn(List.of());

            // itemNo=2 短收：應收 5，實收 3
            ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                    new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8),
                    new ReceiveFactoryDeliveryOrderRequest.Detail(2, 3));

            service.receive(request);

            ArgumentCaptor<FactoryDeliveryOrder> fdoCaptor = ArgumentCaptor.forClass(FactoryDeliveryOrder.class);
            Mockito.verify(fdoRepo).save(fdoCaptor.capture());
            Assertions.assertEquals(FactoryDeliveryStatus.DISCREPANCY, fdoCaptor.getValue().getStatus());

            ArgumentCaptor<BranchPurchaseOrder> bpoCaptor = ArgumentCaptor.forClass(BranchPurchaseOrder.class);
            Mockito.verify(bpoRepo).save(bpoCaptor.capture());
            Assertions.assertEquals(BpoStatus.DISCREPANCY, bpoCaptor.getValue().getStatus());

            // 關鍵：兩筆都要入庫，且用「實收量」不是「應收量」
            Mockito.verify(inventoryService).receive(BRANCH, "P001", BATCH_NO, EXPIRY, 8, FDO_NO);
            Mockito.verify(inventoryService).receive(BRANCH, "P002", BATCH_NO, EXPIRY, 3, FDO_NO);
        } finally {
            UserContextHolder.clear();
        }
    }

    @Test
    void receive_whenUpstreamBpoNotFound_throwsIllegalStateException() {
        Mockito.when(fdoRepo.findByFdoNo(FDO_NO)).thenReturn(Optional.of(fdo(FactoryDeliveryStatus.PENDING)));
        Mockito.when(fdodRepo.findByFdoNoOrderByItemNo(FDO_NO)).thenReturn(List.of(fdod(1, "P001", 8)));
        Mockito.when(fdoRepo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        Mockito.when(bpoRepo.findByBpoNo(BPO_NO)).thenReturn(Optional.empty());

        ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8));

        Assertions.assertThrows(IllegalStateException.class, () -> service.receive(request));
    }

    @Test
    void receive_whenRequestHasDuplicatedItemNo_throwsValidationErrorBeforeTouchingDb() {
        // itemNo=1 送兩次：Collectors.toMap 會拋 IllegalStateException 冒成 500，須先擋成 400
        ReceiveFactoryDeliveryOrderRequest request = receiveRequest(
                new ReceiveFactoryDeliveryOrderRequest.Detail(1, 8),
                new ReceiveFactoryDeliveryOrderRequest.Detail(1, 3));

        BusinessException ex = Assertions.assertThrows(BusinessException.class, () -> service.receive(request));

        Assertions.assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        Mockito.verifyNoInteractions(fdoRepo, fdodRepo, bpoRepo, inventoryService);
    }

    private BranchPurchaseOrder bpo() {
        BranchPurchaseOrder bpo = new BranchPurchaseOrder();
        bpo.setBpoNo(BPO_NO);
        bpo.setBranchCode(BRANCH);
        bpo.setFactoryCode(FACTORY);
        bpo.setPurchaseDate(DATE);
        return bpo;
    }

    private BranchPurchaseOrderDetail bpod(int itemNo, String productCode, String productName, String unit, int qty) {
        BranchPurchaseOrderDetail d = new BranchPurchaseOrderDetail();
        d.setBpoNo(BPO_NO);
        d.setItemNo(itemNo);
        d.setProductCode(productCode);
        d.setProductName(productName);
        d.setUnit(unit);
        d.setQty(qty);
        return d;
    }

    private FactoryDeliveryOrder fdo(FactoryDeliveryStatus status) {
        FactoryDeliveryOrder fdo = new FactoryDeliveryOrder();
        fdo.setFdoNo(FDO_NO);
        fdo.setBpoNo(BPO_NO);
        fdo.setBranchCode(BRANCH);
        fdo.setFactoryCode(FACTORY);
        fdo.setDeliveryDate(DATE);
        fdo.setStatus(status);
        return fdo;
    }

    private FactoryDeliveryOrderDetail fdod(int itemNo, String productCode, int qty) {
        FactoryDeliveryOrderDetail d = new FactoryDeliveryOrderDetail();
        d.setFdoNo(FDO_NO);
        d.setItemNo(itemNo);
        d.setProductCode(productCode);
        d.setProductName("商品" + productCode);
        d.setBatchNo(BATCH_NO);
        d.setExpiryDate(EXPIRY);
        d.setUnit("箱");
        d.setQty(qty);
        return d;
    }

    private ReceiveFactoryDeliveryOrderRequest receiveRequest(ReceiveFactoryDeliveryOrderRequest.Detail... details) {
        return new ReceiveFactoryDeliveryOrderRequest(FDO_NO, null, List.of(details));
    }
}
