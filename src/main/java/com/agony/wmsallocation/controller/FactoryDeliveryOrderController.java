package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.receive.FactoryDeliveryOrderDto;
import com.agony.wmsallocation.dto.receive.ReceiveFactoryDeliveryOrderRequest;
import com.agony.wmsallocation.service.FactoryDeliveryOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/factory-delivery-orders")
public class FactoryDeliveryOrderController {

    private final FactoryDeliveryOrderService factoryDeliveryOrderService;

    /** Mock 工廠出貨：依 BPO 產生 FDO。 */
    @PostMapping("/actions/ship")
    public FactoryDeliveryOrderDto ship(@RequestParam String bpoNo) {
        return factoryDeliveryOrderService.ship(bpoNo);
    }

    /** 查詢某營業所待收貨清單。 */
    @GetMapping("/pending")
    public List<FactoryDeliveryOrderDto> listPending(@RequestParam String branchCode) {
        return factoryDeliveryOrderService.listPending(branchCode);
    }

    /** 查詢某營業所收貨記錄（已收貨、有差異）。 */
    @GetMapping("/received")
    public List<FactoryDeliveryOrderDto> listReceived(@RequestParam String branchCode) {
        return factoryDeliveryOrderService.listReceived(branchCode);
    }

    /** 查詢單一 FDO 明細。 */
    @GetMapping
    public FactoryDeliveryOrderDto getByFdoNo(@RequestParam String fdoNo) {
        return factoryDeliveryOrderService.getByFdoNo(fdoNo);
    }

    /** 收貨確認：逐項輸入實收數量，比對後轉態並入庫。 */
    @PostMapping("/actions/receive")
    public FactoryDeliveryOrderDto receive(@Valid @RequestBody ReceiveFactoryDeliveryOrderRequest request) {
        return factoryDeliveryOrderService.receive(request);
    }
}
