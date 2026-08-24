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
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.FactoryDeliveryOrderMapper;
import com.agony.wmsallocation.repository.BranchPurchaseOrderDetailRepo;
import com.agony.wmsallocation.repository.BranchPurchaseOrderRepo;
import com.agony.wmsallocation.repository.FactoryDeliveryOrderDetailRepo;
import com.agony.wmsallocation.repository.FactoryDeliveryOrderRepo;
import com.agony.wmsallocation.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Mock 工廠出貨：依一張營業所訂貨單（BPO）產生對應工廠出貨單（FDO）。
 *
 * <p>業務規則詳見 {@code docs/requirements/specification/receive/FactoryDeliveryOrder.md}「單據來源」一節。
 * <p>明細以業務單號 fdoNo 關聯、分開 save，與 BPO 彙總一致，不使用 JPA 關聯 cascade。
 */
@RequiredArgsConstructor
@Service
public class FactoryDeliveryOrderService {

    private final BranchPurchaseOrderRepo bpoRepo;
    private final BranchPurchaseOrderDetailRepo bpodRepo;
    private final FactoryDeliveryOrderRepo fdoRepo;
    private final FactoryDeliveryOrderDetailRepo fdodRepo;
    private final SequenceService sequenceService;
    private final FactoryDeliveryOrderMapper mapper;
    private final InventoryService inventoryService;
    private final Clock clock;

    /**
     * 依 BPO 全額出貨產生 FDO（單批次模擬）。一張 BPO 僅能出貨一次。
     */
    @Transactional
    public FactoryDeliveryOrderDto ship(String bpoNo) {
        BranchPurchaseOrder bpo = bpoRepo.findByBpoNo(bpoNo)
                .orElseThrow(() -> new BusinessRuleException("查無營業所訂貨單：" + bpoNo, ErrorCode.RESOURCE_NOT_FOUND));

        if (fdoRepo.existsByBpoNo(bpoNo)) {
            throw new BusinessRuleException("該訂貨單已出貨，不可重複出貨：" + bpoNo, ErrorCode.FDO_ALREADY_SHIPPED);
        }

        List<BranchPurchaseOrderDetail> bpods = bpodRepo.findByBpoNoOrderByItemNo(bpoNo);
        if (bpods.isEmpty()) {
            throw new BusinessRuleException("訂貨單無明細，無法出貨：" + bpoNo, ErrorCode.RESOURCE_NOT_FOUND);
        }

        LocalDate deliveryDate = bpo.getPurchaseDate();

        FactoryDeliveryOrder fdo = new FactoryDeliveryOrder();
        fdo.setFdoNo(sequenceService.generateSequence(SequenceType.FDO, deliveryDate));
        fdo.setBpoNo(bpoNo);
        fdo.setBranchCode(bpo.getBranchCode());
        fdo.setFactoryCode(bpo.getFactoryCode());
        fdo.setDeliveryDate(deliveryDate);
        fdo.setStatus(FactoryDeliveryStatus.PENDING);
        fdo = fdoRepo.save(fdo);   // 先存單頭拿 fdoNo，明細再以此關聯

        // ponytail: 單批次 mock（batchNo=工廠+出貨日、效期=+半年）；配貨 FIFO 需要多批次時改成 ship 請求帶入（決策 B）
        String batchNo = fdo.getFactoryCode() + "-" + deliveryDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate expiryDate = deliveryDate.plusMonths(6);

        List<FactoryDeliveryOrderDetail> details = new ArrayList<>();
        for (BranchPurchaseOrderDetail bpod : bpods) {
            FactoryDeliveryOrderDetail d = new FactoryDeliveryOrderDetail();
            d.setFdoNo(fdo.getFdoNo());
            d.setItemNo(bpod.getItemNo());
            d.setProductCode(bpod.getProductCode());
            d.setProductName(bpod.getProductName());
            d.setBatchNo(batchNo);
            d.setExpiryDate(expiryDate);
            d.setUnit(bpod.getUnit());
            d.setQty(bpod.getQty());   // 全額出貨；receivedQty 收貨階段才填
            details.add(d);
        }
        fdodRepo.saveAll(details);

        return toDto(fdo, details);
    }

    /**
     * 收貨確認：逐項比對出貨數量與實收數量，相符轉 RECEIVED、不符轉 DISCREPANCY，
     * 同步上游 BPO 狀態，並依實收數量入庫。
     */
    @Transactional
    public FactoryDeliveryOrderDto receive(ReceiveFactoryDeliveryOrderRequest request) {
        // trust boundary：itemNo 由呼叫端帶，重複會讓下面的 toMap 拋 IllegalStateException 冒成 500。
        // 這是請求本身的形狀錯誤（@Valid 管不到跨欄位重複），故在碰 DB 前先擋掉、回 400。
        if (request.details().stream().map(ReceiveFactoryDeliveryOrderRequest.Detail::itemNo).distinct().count()
                != request.details().size()) {
            throw new BusinessRuleException("收貨明細 itemNo 重複：" + request.fdoNo(), ErrorCode.VALIDATION_ERROR);
        }

        FactoryDeliveryOrder fdo = fdoRepo.findByFdoNo(request.fdoNo())
                .orElseThrow(() -> new BusinessRuleException("查無工廠出貨單：" + request.fdoNo(), ErrorCode.RESOURCE_NOT_FOUND));

        if (fdo.getStatus() != FactoryDeliveryStatus.PENDING) {
            throw new BusinessRuleException("工廠出貨單非待收貨狀態，不可收貨確認：" + request.fdoNo(), ErrorCode.FDO_NOT_RECEIVABLE);
        }

        List<FactoryDeliveryOrderDetail> details = fdodRepo.findByFdoNoOrderByItemNo(request.fdoNo());

        Map<Integer, Integer> receivedQtyByItemNo = request.details().stream()
                .collect(Collectors.toMap(ReceiveFactoryDeliveryOrderRequest.Detail::itemNo,
                        ReceiveFactoryDeliveryOrderRequest.Detail::receivedQty));

        boolean allMatch = true;
        for (FactoryDeliveryOrderDetail detail : details) {
            Integer receivedQty = receivedQtyByItemNo.get(detail.getItemNo());
            if (receivedQty == null) {
                throw new BusinessRuleException("明細缺少實收數量：itemNo=" + detail.getItemNo(), ErrorCode.VALIDATION_ERROR);
            }
            detail.setReceivedQty(receivedQty);
            if (!receivedQty.equals(detail.getQty())) {
                allMatch = false;
            }
        }
        fdodRepo.saveAll(details);

        FactoryDeliveryStatus resultStatus = allMatch ? FactoryDeliveryStatus.RECEIVED : FactoryDeliveryStatus.DISCREPANCY;
        fdo.setStatus(resultStatus);
        fdo.setReceivedAt(LocalDateTime.now(clock));
        fdo.setReceivedBy(UserContextHolder.getUserCode());
        fdo.setRemark(request.remark());
        fdo = fdoRepo.save(fdo);

        // BPO 理論上一定存在（ship() 建立 FDO 時已驗證過），查無代表資料不一致，非使用者能觸發的業務錯誤
        String bpoNo = fdo.getBpoNo();
        BranchPurchaseOrder bpo = bpoRepo.findByBpoNo(bpoNo)
                .orElseThrow(() -> new IllegalStateException("資料不一致：FDO 對應的 BPO 不存在：" + bpoNo));
        bpo.setStatus(resultStatus == FactoryDeliveryStatus.RECEIVED ? BpoStatus.RECEIVED : BpoStatus.DISCREPANCY);
        bpoRepo.save(bpo);

        // 不論整單相符與否，均依實收數量入庫（含 0：整項未到貨也要如實反映）
        for (FactoryDeliveryOrderDetail detail : details) {
            inventoryService.receive(fdo.getBranchCode(), detail.getProductCode(), detail.getBatchNo(),
                    detail.getExpiryDate(), detail.getReceivedQty(), fdo.getFdoNo());
        }

        return toDto(fdo, details);
    }

    /** 查詢某營業所待收貨清單（status = PENDING）。 */
    public List<FactoryDeliveryOrderDto> listPending(String branchCode) {
        return fdoRepo.findByBranchCodeAndStatusIn(branchCode, List.of(FactoryDeliveryStatus.PENDING)).stream()
                .map(fdo -> toDto(fdo, fdodRepo.findByFdoNoOrderByItemNo(fdo.getFdoNo())))
                .toList();
    }

    /** 查詢某營業所收貨記錄（status = RECEIVED 或 DISCREPANCY）。 */
    public List<FactoryDeliveryOrderDto> listReceived(String branchCode) {
        return fdoRepo.findByBranchCodeAndStatusIn(branchCode,
                        List.of(FactoryDeliveryStatus.RECEIVED, FactoryDeliveryStatus.DISCREPANCY)).stream()
                .map(fdo -> toDto(fdo, fdodRepo.findByFdoNoOrderByItemNo(fdo.getFdoNo())))
                .toList();
    }

    /** 查詢單一 FDO 明細。 */
    public FactoryDeliveryOrderDto getByFdoNo(String fdoNo) {
        FactoryDeliveryOrder fdo = fdoRepo.findByFdoNo(fdoNo)
                .orElseThrow(() -> new BusinessRuleException("查無工廠出貨單：" + fdoNo, ErrorCode.RESOURCE_NOT_FOUND));
        return toDto(fdo, fdodRepo.findByFdoNoOrderByItemNo(fdoNo));
    }

    private FactoryDeliveryOrderDto toDto(FactoryDeliveryOrder fdo, List<FactoryDeliveryOrderDetail> details) {
        FactoryDeliveryOrderDto dto = mapper.toDto(fdo);
        dto.setDetails(mapper.toDetailDtoList(details));
        return dto;
    }
}
