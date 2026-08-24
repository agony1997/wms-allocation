package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.allocation.AllocationOrderDetailDto;
import com.agony.wmsallocation.dto.allocation.AllocationOrderDto;
import com.agony.wmsallocation.dto.purchase.SalesPurchaseOrderDetailDto;
import com.agony.wmsallocation.entity.allocation.enums.AllocationStatus;
import com.agony.wmsallocation.entity.purchase.enums.SalesOrderDetailStatus;
import com.agony.wmsallocation.security.JwtInterceptor;
import com.agony.wmsallocation.service.AllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link AllocationOrderController} 的 Web 層測試。
 *
 * <p>聚焦路由分派、成功路徑的 HTTP status 與 DTO 序列化、參數是否正確轉呼叫 service。
 * 端點皆無 {@code @Valid}（純 {@code @RequestParam}/{@code @PathVariable}），
 * 錯誤格式已由 {@code GlobalExceptionHandlerTest} 集中測，不重複。
 */
@WebMvcTest(AllocationOrderController.class)
class AllocationOrderControllerTest {

    private static final String BRANCH = "B001";
    private static final String DATE = "2026-07-15";
    private static final String ALLOCATION_NO = "AO-20260715-001";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AllocationService allocationService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void setUp() {
        when(jwtInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    private AllocationOrderDetailDto detailDto() {
        return AllocationOrderDetailDto.builder()
                .allocationNo(ALLOCATION_NO)
                .itemNo(1)
                .locationCode("S001")
                .productCode("P001")
                .batchNo("BATCH01")
                .expiryDate(LocalDate.of(2026, 12, 31))
                .requestedQty(10)
                .allocatedQty(10)
                .status(AllocationStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("GET /api/allocation-orders/pending-spod - 應回傳待配 SPOD 清單")
    void listPendingSpod_shouldReturn200WithList() throws Exception {
        SalesPurchaseOrderDetailDto spod = SalesPurchaseOrderDetailDto.builder()
                .productCode("P001")
                .productName("測試商品")
                .unit("箱")
                .confirmedQty(10)
                .status(SalesOrderDetailStatus.AGGREGATED)
                .build();
        when(allocationService.listPendingSpod(BRANCH)).thenReturn(List.of(spod));

        mockMvc.perform(get("/api/allocation-orders/pending-spod").param("branchCode", BRANCH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("P001"))
                .andExpect(jsonPath("$[0].status").value("AGGREGATED"));

        verify(allocationService).listPendingSpod(BRANCH);
    }

    @Test
    @DisplayName("POST /api/allocation-orders/actions/allocate - 應帶參數呼叫 service 並回傳配貨明細")
    void allocate_shouldCallServiceAndReturnDetails() throws Exception {
        when(allocationService.executeAllocation(BRANCH, LocalDate.parse(DATE))).thenReturn(List.of(detailDto()));

        mockMvc.perform(post("/api/allocation-orders/actions/allocate")
                        .param("branchCode", BRANCH)
                        .param("allocationDate", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allocationNo").value(ALLOCATION_NO))
                .andExpect(jsonPath("$[0].productCode").value("P001"));

        verify(allocationService).executeAllocation(BRANCH, LocalDate.parse(DATE));
    }

    @Test
    @DisplayName("POST /api/allocation-orders/actions/allocate 無可配貨 - 應回傳空清單")
    void allocate_whenNothingToAllocate_shouldReturnEmptyList() throws Exception {
        when(allocationService.executeAllocation(BRANCH, LocalDate.parse(DATE))).thenReturn(List.of());

        mockMvc.perform(post("/api/allocation-orders/actions/allocate")
                        .param("branchCode", BRANCH)
                        .param("allocationDate", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET /api/allocation-orders - 應帶參數呼叫 service 並回傳配貨單清單")
    void list_shouldCallServiceAndReturnOrders() throws Exception {
        AllocationOrderDto dto = AllocationOrderDto.builder()
                .allocationNo(ALLOCATION_NO)
                .branchCode(BRANCH)
                .allocationDate(LocalDate.parse(DATE))
                .status(AllocationStatus.PENDING)
                .details(List.of(detailDto()))
                .build();
        when(allocationService.list(BRANCH, LocalDate.parse(DATE))).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/allocation-orders")
                        .param("branchCode", BRANCH)
                        .param("allocationDate", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].allocationNo").value(ALLOCATION_NO))
                .andExpect(jsonPath("$[0].details[0].productCode").value("P001"));

        verify(allocationService).list(BRANCH, LocalDate.parse(DATE));
    }

    @Test
    @DisplayName("GET /api/allocation-orders/{allocationNo} - 應回傳單一配貨單明細")
    void get_shouldReturnSingleOrder() throws Exception {
        AllocationOrderDto dto = AllocationOrderDto.builder()
                .allocationNo(ALLOCATION_NO)
                .branchCode(BRANCH)
                .allocationDate(LocalDate.parse(DATE))
                .status(AllocationStatus.PENDING)
                .details(List.of(detailDto()))
                .build();
        when(allocationService.get(ALLOCATION_NO)).thenReturn(dto);

        mockMvc.perform(get("/api/allocation-orders/{allocationNo}", ALLOCATION_NO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allocationNo").value(ALLOCATION_NO))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.details[0].batchNo").value("BATCH01"));
    }
}
