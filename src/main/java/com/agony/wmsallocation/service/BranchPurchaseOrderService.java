package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.purchase.BranchPurchaseOrderDto;
import com.agony.wmsallocation.entity.master.Product;
import com.agony.wmsallocation.entity.master.ProductFactory;
import com.agony.wmsallocation.entity.purchase.*;
import com.agony.wmsallocation.entity.purchase.enums.BpoStatus;
import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.BranchPurchaseOrderMapper;
import com.agony.wmsallocation.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 業務規則詳見 {@code docs/requirements/specification/purchase/BranchPurchase.md}「庫務彙總作業」一節。
 */
@RequiredArgsConstructor
@Service
public class BranchPurchaseOrderService {

    private final BranchPurchaseFrozenRepo bpfRepo;
    private final SalesPurchaseOrderRepo spoRepo;
    private final SalesPurchaseOrderDetailRepo spodRepo;
    private final ProductFactoryRepo productFactoryRepo;
    private final ProductRepo productRepo;
    private final BranchPurchaseOrderRepo bpoRepo;
    private final BranchPurchaseOrderDetailRepo bpodRepo;
    private final SequenceService sequenceService;
    private final BranchPurchaseOrderMapper mapper;

    @Transactional
    public List<BranchPurchaseOrderDto> aggregate(String branchCode, LocalDate purchaseDate) {
        BranchPurchaseFrozen bpf = bpfRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate)
                .orElseThrow(() -> new BusinessRuleException("尚未凍結，無法彙總", ErrorCode.RESOURCE_NOT_FOUND));
        if (bpf.getStatus() != FrozenStatus.CONFIRMED) {
            throw new BusinessRuleException("尚未確認完成，無法彙總", ErrorCode.PURCHASE_ORDER_NOT_EDITABLE);
        }

        List<String> purchaseNos = spoRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate).stream()
                .map(SalesPurchaseOrder::getPurchaseNo)
                .toList();

        List<SalesPurchaseOrderDetail> pending = purchaseNos.isEmpty()
                ? List.of()
                : spodRepo.findByPurchaseNoInAndStatus(purchaseNos, SalesOrderDetailStatus.PENDING);

        if (pending.isEmpty()) {
            return List.of();   // 已彙總過或當天無訂單，冪等返回
        }

        Set<String> productCodes = pending.stream().map(SalesPurchaseOrderDetail::getProductCode).collect(Collectors.toSet());

        // 全部驗證通過才動手：避免中途才發現缺漏，前面工廠已呼叫 SequenceService（REQUIRES_NEW 已提交）白耗單號
        Map<String, String> factoryByProduct = productFactoryRepo.findByProductCodeInAndIsDefaultTrue(new ArrayList<>(productCodes)).stream()
                .collect(Collectors.toMap(ProductFactory::getProductCode, ProductFactory::getFactoryCode, (a, b) -> a));

        Set<String> missingFactory = productCodes.stream().filter(pc -> !factoryByProduct.containsKey(pc)).collect(Collectors.toSet());

        if (!missingFactory.isEmpty()) {
            throw new BusinessRuleException("商品缺少預設工廠對應：" + missingFactory, ErrorCode.PRODUCT_FACTORY_NOT_CONFIGURED);
        }

        Map<String, String> productNameByCode = productRepo.findByProductCodeIn(new ArrayList<>(productCodes)).stream()
                .collect(Collectors.toMap(Product::getProductCode, Product::getProductName));
        Set<String> missingProduct = productCodes.stream().filter(pc -> !productNameByCode.containsKey(pc)).collect(Collectors.toSet());

        if (!missingProduct.isEmpty()) {
            throw new BusinessRuleException("商品主檔查無資料：" + missingProduct, ErrorCode.PRODUCT_FACTORY_NOT_CONFIGURED);
        }

        Map<String, List<SalesPurchaseOrderDetail>> byFactory = pending.stream()
                .collect(Collectors.groupingBy(d -> factoryByProduct.get(d.getProductCode())));

        List<BranchPurchaseOrderDto> result = new ArrayList<>();
        for (var entry : byFactory.entrySet()) {
            BranchPurchaseOrder bpo = new BranchPurchaseOrder();
            bpo.setBpoNo(sequenceService.generateSequence(SequenceType.BPO, purchaseDate));
            bpo.setBranchCode(branchCode);
            bpo.setFactoryCode(entry.getKey());
            bpo.setPurchaseDate(purchaseDate);
            bpo.setStatus(BpoStatus.PENDING);
            bpo = bpoRepo.save(bpo);

            // 加總的是 confirmedQty（組長確認數），不是 qty（業務員原始訂購數）
            Map<ProductUnitKey, Integer> qtyByProductUnit = entry.getValue().stream()
                    .collect(Collectors.groupingBy(d -> new ProductUnitKey(d.getProductCode(), d.getUnit()),
                            Collectors.summingInt(SalesPurchaseOrderDetail::getConfirmedQty)));

            List<BranchPurchaseOrderDetail> bpods = new ArrayList<>();
            int itemNo = 1;
            for (var e : qtyByProductUnit.entrySet().stream()
                    .sorted(Comparator.comparing(x -> x.getKey().productCode())).toList()) {
                BranchPurchaseOrderDetail bpod = new BranchPurchaseOrderDetail();
                bpod.setBpoNo(bpo.getBpoNo());
                bpod.setItemNo(itemNo++);
                bpod.setProductCode(e.getKey().productCode());
                bpod.setUnit(e.getKey().unit());
                bpod.setProductName(productNameByCode.get(e.getKey().productCode())); // 絕不可從 SPOD 複製，該欄位實務上永遠是空的
                bpod.setQty(e.getValue());
                bpods.add(bpod);
            }
            bpodRepo.saveAll(bpods);

            // 該工廠組的 SPOD 標記已彙總並回填來源 BPO 單號（供配貨查詢「對應 BPO 已收貨」）
            String bpoNo = bpo.getBpoNo();
            entry.getValue().forEach(d -> {
                d.setStatus(SalesOrderDetailStatus.AGGREGATED);
                d.setBpoNo(bpoNo);
            });

            BranchPurchaseOrderDto dto = mapper.toDto(bpo);
            dto.setDetails(mapper.toDetailDtoList(bpods));
            result.add(dto);
        }

        spodRepo.saveAll(pending);
        return result;
    }

    public List<BranchPurchaseOrderDto> list(String branchCode, LocalDate purchaseDate) {
        return bpoRepo.findByBranchCodeAndPurchaseDate(branchCode, purchaseDate).stream()
                .map(bpo -> {
                    BranchPurchaseOrderDto dto = mapper.toDto(bpo);
                    dto.setDetails(mapper.toDetailDtoList(bpodRepo.findByBpoNoOrderByItemNo(bpo.getBpoNo())));
                    return dto;
                }).toList();
    }

    private record ProductUnitKey(String productCode, String unit) {}
}
