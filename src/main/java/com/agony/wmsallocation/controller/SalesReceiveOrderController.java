package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.allocation.AllocationOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.SalesReceiveOrderDto;
import com.agony.wmsallocation.service.SalesReceiveOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sales-receive-orders")
public class SalesReceiveOrderController {

    private final SalesReceiveOrderService salesReceiveOrderService;

    /** 查詢某業務員儲位的待領明細（領貨前點貨用）。 */
    @GetMapping("/pending")
    public List<AllocationOrderDetailDto> listPending(@RequestParam String locationCode) {
        return salesReceiveOrderService.listPending(locationCode);
    }

    /**
     * 確認領貨：該儲位待領明細一次領完，回傳本次產生的領貨明細（無待領時回空清單）。
     * branchCode 由儲位主檔反查、領貨日期由後端取當下，皆不由呼叫端指定。
     */
    @PostMapping("/actions/receive")
    public List<SalesReceiveOrderDetailDto> receive(@RequestParam String locationCode) {
        return salesReceiveOrderService.receive(locationCode);
    }

    /** 查詢某營業所某日的領貨單清單。 */
    @GetMapping
    public List<SalesReceiveOrderDto> list(
            @RequestParam String branchCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDate) {
        return salesReceiveOrderService.list(branchCode, receiveDate);
    }

    /** 查詢單一領貨單明細。 */
    @GetMapping("/{receiveNo}")
    public SalesReceiveOrderDto get(@PathVariable String receiveNo) {
        return salesReceiveOrderService.get(receiveNo);
    }
}
