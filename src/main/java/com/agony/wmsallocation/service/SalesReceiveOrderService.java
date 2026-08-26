package com.agony.wmsallocation.service;

import com.agony.wmsallocation.dto.allocation.AllocationOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDto;
import com.agony.wmsallocation.entity.allocation.AllocationOrderDetail;
import com.agony.wmsallocation.entity.allocation.SalesReceiveOrder;
import com.agony.wmsallocation.entity.allocation.SalesReceiveOrderDetail;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import com.agony.wmsallocation.entity.sequence.enums.SequenceType;
import com.agony.wmsallocation.exception.BusinessRuleException;
import com.agony.wmsallocation.exception.ErrorCode;
import com.agony.wmsallocation.mapper.AllocationOrderMapper;
import com.agony.wmsallocation.mapper.SalesReceiveOrderMapper;
import com.agony.wmsallocation.repository.AllocationOrderDetailRepo;
import com.agony.wmsallocation.repository.AllocationOrderRepo;
import com.agony.wmsallocation.repository.LocationRepo;
import com.agony.wmsallocation.repository.SalesReceiveOrderDetailRepo;
import com.agony.wmsallocation.repository.SalesReceiveOrderRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 領貨 Service：業務員把已配貨明細 (AOD) 領至自己儲位。
 * 業務規則詳見 {@code docs/requirements/specification/allocation/SalesReceiveOrder.md}。
 *
 * <p>庫存語意：大庫已在配貨時扣減（預留），領貨只做「車存 +」，不再動大庫。
 * 也就是餘額表的 {@code qty} 是「可再配的量」而非架上實體量，已配未領的部分
 * 隱含在 {@code AOD.status = PENDING} 的列裡。
 */
@RequiredArgsConstructor
@Service
public class SalesReceiveOrderService {

    private final AllocationOrderRepo aoRepo;
    private final AllocationOrderDetailRepo aodRepo;
    private final SalesReceiveOrderRepo sroRepo;
    private final SalesReceiveOrderDetailRepo srodRepo;
    private final LocationRepo locationRepo;
    private final SequenceService sequenceService;
    private final InventoryService inventoryService;
    private final SalesReceiveOrderMapper mapper;
    private final AllocationOrderMapper allocationOrderMapper;
    private final Clock clock;

    /** 查詢某業務員儲位的待領明細（唯讀預覽，不上鎖）。 */
    public List<AllocationOrderDetailDto> listPending(String locationCode) {
        return allocationOrderMapper.toDetailDtoList(
                aodRepo.findByLocationCodeAndStatusOrderByAllocationNoAscItemNoAsc(
                        locationCode, AllocationStatus.PENDING));
    }

    /**
     * 確認領貨：該儲位所有待領明細一次領完（2026-08-26 定案，見規格「領貨範圍」）。
     * 無待領明細時冪等返回空清單，不取號、不建單。
     *
     * <p>只收 locationCode——它全域唯一，branchCode 由儲位主檔反查，
     * 呼叫端無從指定成別的營業所（同 B2 的作法：能推導的身分不開放外部指定）。
     */
    @Transactional
    public List<SalesReceiveOrderDetailDto> receive(String locationCode) {
        // 1. 待領明細（上悲觀鎖，避免連點領兩次）
        List<AllocationOrderDetail> pending =
                aodRepo.findForUpdateByLocationCodeAndStatusOrderByAllocationNoAscItemNoAsc(
                        locationCode, AllocationStatus.PENDING);
        if (pending.isEmpty()) {
            return List.of();
        }

        String branchCode = locationRepo.findByLocationCode(locationCode)
                .map(location -> location.getBranchCode())
                .orElseThrow(() -> new BusinessRuleException("找不到儲位：locationCode=" + locationCode,
                        ErrorCode.RESOURCE_NOT_FOUND));

        // 2. 取號建 SRO。receiveDate 取自注入的 Clock——領貨是「當下」發生的動作，
        //    沒有讓呼叫端指定日期的業務理由。
        LocalDate receiveDate = LocalDate.now(clock);
        String receiveNo = sequenceService.generateSequence(SequenceType.SRO, receiveDate);

        SalesReceiveOrder sro = new SalesReceiveOrder();
        sro.setReceiveNo(receiveNo);
        sro.setBranchCode(branchCode);
        sro.setLocationCode(locationCode);
        sro.setReceiveDate(receiveDate);
        sroRepo.save(sro);

        // 3. 逐筆建 SROD、轉態、加車存。qty 一律等於 AOD.allocatedQty（AOD : SROD = 1:1，一次領完）
        List<SalesReceiveOrderDetail> details = new ArrayList<>();
        Set<String> touchedAllocationNos = new LinkedHashSet<>();
        int itemNo = 1;

        for (AllocationOrderDetail aod : pending) {
            SalesReceiveOrderDetail srod = new SalesReceiveOrderDetail();
            srod.setReceiveNo(receiveNo);
            srod.setItemNo(itemNo++);
            srod.setAllocationNo(aod.getAllocationNo());
            srod.setAllocationItemNo(aod.getItemNo());
            srod.setProductCode(aod.getProductCode());
            srod.setBatchNo(aod.getBatchNo());
            srod.setExpiryDate(aod.getExpiryDate());
            srod.setQty(aod.getAllocatedQty());
            details.add(srod);

            aod.setStatus(AllocationStatus.RECEIVED);
            touchedAllocationNos.add(aod.getAllocationNo());

            inventoryService.pickUp(branchCode, locationCode, aod.getProductCode(), aod.getBatchNo(),
                    aod.getExpiryDate(), aod.getAllocatedQty(), receiveNo);
        }

        srodRepo.saveAll(details);
        // saveAllAndFlush：下一步的 existsBy 查詢要看得到剛轉好的 RECEIVED 狀態。
        // 靠 FlushMode.AUTO 也會 flush，但那是隱性依賴，這裡寫明比較不會被後人改壞。
        aodRepo.saveAllAndFlush(pending);

        syncAllocationOrderStatus(touchedAllocationNos);

        return mapper.toDetailDtoList(details);
    }

    /** 查詢某營業所某日的領貨單清單（單頭 + 明細）。 */
    public List<SalesReceiveOrderDto> list(String branchCode, LocalDate receiveDate) {
        return sroRepo.findByBranchCodeAndReceiveDate(branchCode, receiveDate).stream()
                .map(sro -> {
                    SalesReceiveOrderDto dto = mapper.toDto(sro);
                    dto.setDetails(mapper.toDetailDtoList(srodRepo.findByReceiveNoOrderByItemNo(sro.getReceiveNo())));
                    return dto;
                }).toList();
    }

    /** 查詢單一領貨單（單頭 + 明細）。 */
    public SalesReceiveOrderDto get(String receiveNo) {
        SalesReceiveOrder sro = sroRepo.findByReceiveNo(receiveNo)
                .orElseThrow(() -> new BusinessRuleException("領貨單不存在：" + receiveNo, ErrorCode.RESOURCE_NOT_FOUND));
        SalesReceiveOrderDto dto = mapper.toDto(sro);
        dto.setDetails(mapper.toDetailDtoList(srodRepo.findByReceiveNoOrderByItemNo(receiveNo)));
        return dto;
    }

    /**
     * 維護 AO 的聚合狀態（AllocationOrder.md 狀態約束）。
     *
     * <p>一張 AO 底下的 AOD 分屬**多個業務員**，本次領貨只轉掉其中一位的部分，
     * 因此不能直接把 AO 轉 RECEIVED——必須逐張回頭確認「已無任何 PENDING 明細」。
     */
    private void syncAllocationOrderStatus(Set<String> allocationNos) {
        for (String allocationNo : allocationNos) {
            if (aodRepo.existsByAllocationNoAndStatus(allocationNo, AllocationStatus.PENDING)) {
                continue;   // 還有別人的明細沒領，維持 PENDING
            }
            aoRepo.findByAllocationNo(allocationNo).ifPresent(ao -> {
                ao.setStatus(AllocationStatus.RECEIVED);
                aoRepo.save(ao);
            });
        }
    }
}
