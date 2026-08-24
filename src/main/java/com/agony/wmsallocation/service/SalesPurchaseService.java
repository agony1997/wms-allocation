package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDto;
import com.agony.wmsallocation.dto.purchase.SavePurchaseRequest;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrder;
import com.agony.wmsallocation.entity.purchase.SalesPurchaseOrderDetail;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.SalesPurchaseOrderMapper;
import com.agony.wmsallocation.repository.BranchPurchaseFrozenRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderDetailRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class SalesPurchaseService {

    private final SalesPurchaseOrderRepo spoRepo;
    private final SalesPurchaseOrderDetailRepo spodRepo;
    private final BranchPurchaseFrozenRepo bpfRepo;
    private final SequenceService sequenceService;
    private final SalesPurchaseOrderMapper mapper;
    private final Clock clock;

    /**
     * 查詢業務員訂貨單（唯讀）。無單則回傳空白表單（purchaseNo=null），**不寫入資料**——
     * 訂單延到首次儲存才建立（lazy create，見 ADR-0009 / SalesPurchase.md）。
     */
    public SalesPurchaseOrderDto find(String branchCode, String locationCode, LocalDate purchaseDate) {

        SalesPurchaseOrderDto orderDto = spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate(branchCode, locationCode, purchaseDate)
                .map(order -> {
                    SalesPurchaseOrderDto dto = mapper.toDto(order);
                    dto.setDetails(mapper.toDetailDtoList(spodRepo.findByPurchaseNoOrderBySortOrder(order.getPurchaseNo())));
                    return dto;
                })
                .orElseGet(() -> SalesPurchaseOrderDto.builder()
                        .branchCode(branchCode)
                        .locationCode(locationCode)
                        .purchaseDate(purchaseDate)
                        .details(List.of())
                        .build());

        bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate)
                .ifPresentOrElse(branchPurchaseFrozen -> orderDto.setEditable(false),
                        () -> orderDto.setEditable(true));

        return orderDto;
    }

    /**
     * 儲存訂貨明細（upsert，業務鍵 = branchCode + locationCode + purchaseDate）：
     * 無單則取號建立、有單則更新。BPF 不存在時業務員才可儲存。
     *
     * <p>必須是單一交易：明細採「先刪後插」整批覆蓋，缺了交易邊界時 delete 與 saveAll
     * 各自提交，saveAll 失敗就會留下「舊明細已刪、新明細沒進」的空單。
     * 取號走 {@code REQUIRES_NEW} 不受本交易回滾影響（見 {@link SequenceService}）。
     */
    @Transactional
    public SalesPurchaseOrderDto save(SavePurchaseRequest request) {
        LocalDate purchaseDate = request.purchaseDate();
        LocalDate today = LocalDate.now(clock);
        // trust boundary：前端 date picker 可繞過，此為最後防線；D+2 下限是 lead time 硬規則
        if (purchaseDate.isBefore(today.plusDays(2)) || purchaseDate.isAfter(today.plusDays(9))) {
            throw new BusinessRuleException(
                    "訂貨日不在合法區間（D+2 ~ D+9）：purchaseDate=" + purchaseDate,
                    ErrorCode.PURCHASE_DATE_OUT_OF_RANGE);
        }

        if (bpfRepo.findByBranchCodeAndPurchaseDate(request.branchCode(), purchaseDate).isPresent()) {
            throw new BusinessRuleException(
                    "營業所當天已凍結，不可編輯：branchCode=" + request.branchCode() + ", purchaseDate=" + purchaseDate,
                    ErrorCode.PURCHASE_ORDER_NOT_EDITABLE);
        }

        SalesPurchaseOrder order = spoRepo.findByBranchCodeAndLocationCodeAndPurchaseDate(
                        request.branchCode(), request.locationCode(), purchaseDate)
                .orElseGet(() -> {
                    SalesPurchaseOrder newOrder = new SalesPurchaseOrder();
                    newOrder.setPurchaseNo(sequenceService.generateSequence(SequenceType.SPO, purchaseDate));
                    newOrder.setBranchCode(request.branchCode());
                    newOrder.setLocationCode(request.locationCode());
                    newOrder.setPurchaseDate(purchaseDate);
                    newOrder.setPurchaseUser(request.purchaseUser());   // 單所屬業務員，非操作者
                    return newOrder;
                });

        order = spoRepo.save(order);

        spodRepo.deleteByPurchaseNo(order.getPurchaseNo());   // 整批覆蓋：先清舊明細
        List<SalesPurchaseOrderDetail> details = toDetailEntities(order.getPurchaseNo(), request.details());
        spodRepo.saveAll(details);

        SalesPurchaseOrderDto dto = mapper.toDto(order);
        dto.setEditable(true);   // 能走到這裡代表 BPF 不存在，必為可編輯
        dto.setDetails(mapper.toDetailDtoList(details));
        return dto;
    }

    private List<SalesPurchaseOrderDetail> toDetailEntities(String purchaseNo, List<SavePurchaseRequest.Detail> details) {
        List<SalesPurchaseOrderDetail> entities = new ArrayList<>();
        int itemNo = 1;
        for (SavePurchaseRequest.Detail d : details) {
            SalesPurchaseOrderDetail entity = new SalesPurchaseOrderDetail();
            entity.setPurchaseNo(purchaseNo);
            entity.setItemNo(itemNo);
            entity.setProductCode(d.productCode());
            // ponytail: productName 待接 Product 主檔查詢補齊，目前留空；前端需要品名顯示時再加 ProductRepo 依賴
            entity.setUnit(d.unit());
            entity.setQty(d.qty());
            entity.setConfirmedQty(d.qty());   // 建立時複製 qty，組長凍結後才會出現差異
            entity.setStatus(SalesOrderDetailStatus.PENDING);
            entity.setSortOrder(d.sortOrder() != null ? d.sortOrder() : itemNo);
            entities.add(entity);
            itemNo++;
        }
        return entities;
    }
}
