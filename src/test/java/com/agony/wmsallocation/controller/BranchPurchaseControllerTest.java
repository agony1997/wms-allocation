package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.purchase.AdjustConfirmedQtyRequest;
import com.agony.wmsallocation.dto.purchase.BranchPurchaseSummaryDto;
import com.agony.wmsallocation.entity.purchase.enums.FrozenStatus;
import com.agony.wmsallocation.security.JwtInterceptor;
import com.agony.wmsallocation.service.BranchPurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BranchPurchaseController} 的 Web 層測試。
 */
@WebMvcTest(BranchPurchaseController.class)
class BranchPurchaseControllerTest {

    private static final String BRANCH = "B01";
    private static final String DATE = "2026-07-06";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BranchPurchaseService branchPurchaseService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void setUp() {
        when(jwtInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/branch-purchases - 應回傳彙總 DTO")
    void getBranchSummary_shouldReturn200WithDto() throws Exception {
        BranchPurchaseSummaryDto dto = BranchPurchaseSummaryDto.builder()
                .branchCode(BRANCH)
                .purchaseDate(LocalDate.parse(DATE))
                .frozenStatus(FrozenStatus.FROZEN)
                .orders(List.of())
                .build();
        when(branchPurchaseService.getBranchSummary(BRANCH, LocalDate.parse(DATE))).thenReturn(dto);

        mockMvc.perform(get("/api/branch-purchases").param("branchCode", BRANCH).param("purchaseDate", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchCode").value(BRANCH))
                .andExpect(jsonPath("$.frozenStatus").value("FROZEN"));
    }

    @Test
    @DisplayName("POST /api/branch-purchases/actions/freeze - 應呼叫 service")
    void freeze_shouldCallService() throws Exception {
        mockMvc.perform(post("/api/branch-purchases/actions/freeze")
                        .param("branchCode", BRANCH)
                        .param("purchaseDate", DATE))
                .andExpect(status().isOk());

        verify(branchPurchaseService).freeze(BRANCH, LocalDate.parse(DATE));
    }

    @Test
    @DisplayName("POST /api/branch-purchases/actions/freeze - 帶 operatorId 也不影響（操作者取自 token，參數已移除）")
    void freeze_ignoresClientSuppliedOperatorId() throws Exception {
        mockMvc.perform(post("/api/branch-purchases/actions/freeze")
                        .param("branchCode", BRANCH)
                        .param("purchaseDate", DATE)
                        .param("operatorId", "SOMEONE_ELSE"))
                .andExpect(status().isOk());

        verify(branchPurchaseService).freeze(BRANCH, LocalDate.parse(DATE));
    }

    @Test
    @DisplayName("POST /api/branch-purchases/actions/unfreeze - 應呼叫 service")
    void unfreeze_shouldCallService() throws Exception {
        mockMvc.perform(post("/api/branch-purchases/actions/unfreeze")
                        .param("branchCode", BRANCH)
                        .param("purchaseDate", DATE))
                .andExpect(status().isOk());

        verify(branchPurchaseService).unfreeze(BRANCH, LocalDate.parse(DATE));
    }

    @Test
    @DisplayName("POST /api/branch-purchases/actions/confirm - 應呼叫 service")
    void confirm_shouldCallService() throws Exception {
        mockMvc.perform(post("/api/branch-purchases/actions/confirm")
                        .param("branchCode", BRANCH)
                        .param("purchaseDate", DATE))
                .andExpect(status().isOk());

        verify(branchPurchaseService).confirm(BRANCH, LocalDate.parse(DATE));
    }

    @Test
    @DisplayName("PUT /api/branch-purchases/adjust - 應轉呼叫 service.adjustConfirmedQty")
    void adjustConfirmedQty_shouldCallService() throws Exception {
        AdjustConfirmedQtyRequest request = new AdjustConfirmedQtyRequest();
        request.setAdjustments(List.of(new AdjustConfirmedQtyRequest.Detail("L01", "P001", "箱", 10)));

        mockMvc.perform(put("/api/branch-purchases/adjust")
                        .param("branchCode", BRANCH)
                        .param("purchaseDate", DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(branchPurchaseService).adjustConfirmedQty(eq(BRANCH), eq(LocalDate.parse(DATE)), any(AdjustConfirmedQtyRequest.class));
    }

    @Test
    @DisplayName("PUT /api/branch-purchases/adjust 缺必填欄位 - 應掛 @Valid 回傳 400 + VALIDATION_ERROR")
    void adjustConfirmedQty_whenMissingRequired_shouldReturn400() throws Exception {
        String invalidJson = "{\"adjustments\":[{\"locationCode\":\"L01\",\"productCode\":\"P001\"}]}";

        mockMvc.perform(put("/api/branch-purchases/adjust")
                        .param("branchCode", BRANCH)
                        .param("purchaseDate", DATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
