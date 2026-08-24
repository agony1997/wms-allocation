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
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.SalesPurchaseOrderMapper;
import com.agony.wmsallocation.repository.BranchPurchaseFrozenRepo;
import com.agony.wmsallocation.repository.ProductRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderDetailRepo;
import com.agony.wmsallocation.repository.SalesPurchaseOrderRepo;
import com.agony.wmsallocation.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BranchPurchaseService {

    private final BranchPurchaseFrozenRepo bpfRepo;
    private final SalesPurchaseOrderRepo spoRepo;
    private final SalesPurchaseOrderDetailRepo spodRepo;
    private final ProductRepo productRepo;
    private final SalesPurchaseOrderMapper mapper;
    private final Clock clock;

    public BranchPurchaseSummaryDto getBranchSummary(String branchCode, LocalDate purchaseDate) {
        List<SalesPurchaseOrder> orders = spoRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate);
        
        List<SalesPurchaseOrderDto> orderDtos = orders.stream().map(order -> {
            SalesPurchaseOrderDto dto = mapper.toDto(order);
            dto.setDetails(mapper.toDetailDtoList(spodRepo.findByPurchaseNoOrderBySortOrder(order.getPurchaseNo())));
            return dto;
        }).toList();

        FrozenStatus status = bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate)
                .map(BranchPurchaseFrozen::getStatus)
                .orElse(null);

        return BranchPurchaseSummaryDto.builder()
                .branchCode(branchCode)
                .purchaseDate(purchaseDate)
                .frozenStatus(status)
                .orders(orderDtos)
                .build();
    }

    /** 凍結；操作者取自當前登入身份（{@link UserContextHolder}），不由呼叫端指定。 */
    @Transactional
    public void freeze(String branchCode, LocalDate purchaseDate) {
        if (bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate).isPresent()) {
             throw new BusinessRuleException("已經凍結或確認", ErrorCode.PURCHASE_ORDER_NOT_EDITABLE);
        }
        BranchPurchaseFrozen bpf = new BranchPurchaseFrozen();
        bpf.setBranchCode(branchCode);
        bpf.setPurchaseDate(purchaseDate);
        bpf.setStatus(FrozenStatus.FROZEN);
        bpf.setFrozenAt(LocalDateTime.now(clock));
        bpf.setFrozenBy(UserContextHolder.getUserCode());
        bpfRepo.save(bpf);
    }

    @Transactional
    public void unfreeze(String branchCode, LocalDate purchaseDate) {
        BranchPurchaseFrozen bpf = bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate)
                .orElseThrow(() -> new BusinessRuleException("查無凍結記錄", ErrorCode.RESOURCE_NOT_FOUND));
        
        if (bpf.getStatus() == FrozenStatus.CONFIRMED) {
             throw new BusinessRuleException("已確認，不可解除凍結", ErrorCode.PURCHASE_ORDER_NOT_EDITABLE);
        }
        bpfRepo.delete(bpf);
    }

    /** 確認；操作者取自當前登入身份（{@link UserContextHolder}），不由呼叫端指定。 */
    @Transactional
    public void confirm(String branchCode, LocalDate purchaseDate) {
        BranchPurchaseFrozen bpf = bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate)
                .orElseThrow(() -> new BusinessRuleException("必須先凍結才能確認", ErrorCode.RESOURCE_NOT_FOUND));

        if (bpf.getStatus() == FrozenStatus.CONFIRMED) {
            return; // 已經確認則冪等返回
        }

        bpf.setStatus(FrozenStatus.CONFIRMED);
        bpf.setConfirmedAt(LocalDateTime.now(clock));
        bpf.setConfirmedBy(UserContextHolder.getUserCode());
        bpfRepo.save(bpf);
    }

    @Transactional
    public void adjustConfirmedQty(String branchCode, LocalDate purchaseDate, AdjustConfirmedQtyRequest request) {
        BranchPurchaseFrozen bpf = bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate)
                .orElseThrow(() -> new BusinessRuleException("尚未凍結，不可修改確認數量", ErrorCode.RESOURCE_NOT_FOUND));
                
        if (bpf.getStatus() == FrozenStatus.CONFIRMED) {
             throw new BusinessRuleException("已確認，不可修改", ErrorCode.PURCHASE_ORDER_NOT_EDITABLE);
        }
        
        // 取得該營業所當天所有的訂單，以便對應 locationCode -> purchaseNo
        List<SalesPurchaseOrder> orders = spoRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate);
        Map<String, String> locationToPurchaseNo = orders.stream()
                .collect(Collectors.toMap(SalesPurchaseOrder::getLocationCode, SalesPurchaseOrder::getPurchaseNo));
        
        for (AdjustConfirmedQtyRequest.Detail adj : request.getAdjustments()) {
            String purchaseNo = locationToPurchaseNo.get(adj.locationCode());
            if (purchaseNo == null) {
                continue;
            }
            List<SalesPurchaseOrderDetail> details = spodRepo.findByPurchaseNoOrderBySortOrder(purchaseNo);
            SalesPurchaseOrderDetail detail = details.stream()
                    .filter(d -> d.getProductCode().equals(adj.productCode()) && d.getUnit().equals(adj.unit()))
                    .findFirst()
                    .orElseGet(() -> newDetail(purchaseNo, details, adj));
            detail.setConfirmedQty(adj.confirmedQty());
            spodRepo.save(detail);
        }
    }

    // 組長新增品項：業務員原始訂購數視為 0，故 confirmedQty - qty 能反映組長加購的量
    private SalesPurchaseOrderDetail newDetail(String purchaseNo, List<SalesPurchaseOrderDetail> existing,
                                                AdjustConfirmedQtyRequest.Detail adj) {
        Product product = productRepo.findByProductCode(adj.productCode())
                .orElseThrow(() -> new BusinessRuleException("商品主檔查無資料：" + adj.productCode(), ErrorCode.PRODUCT_FACTORY_NOT_CONFIGURED));

        int nextItemNo = existing.stream().mapToInt(SalesPurchaseOrderDetail::getItemNo).max().orElse(0) + 1;

        SalesPurchaseOrderDetail detail = new SalesPurchaseOrderDetail();
        detail.setPurchaseNo(purchaseNo);
        detail.setItemNo(nextItemNo);
        detail.setProductCode(adj.productCode());
        detail.setProductName(product.getProductName());
        detail.setUnit(adj.unit());
        detail.setQty(0);
        detail.setStatus(SalesOrderDetailStatus.PENDING);
        detail.setSortOrder(nextItemNo);
        return detail;
    }
}
