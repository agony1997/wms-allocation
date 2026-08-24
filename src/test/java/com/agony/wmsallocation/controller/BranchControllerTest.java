package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.branch.BranchCreateRequest;
import com.agony.wmsallocation.dto.branch.BranchDto;
import com.agony.wmsallocation.dto.branch.BranchUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.service.BranchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link BranchController} 的 Web 層測試。
 *
 * <p>聚焦 controller 自己的責任：路由分派、成功路徑的 HTTP status 與 DTO 序列化、
 * 以及端點確實掛了 {@code @Valid}（驗證真的會觸發）。
 *
 * <p>「例外 → HTTP status → ErrorResponse body」這條泛用對映已由
 * {@code GlobalExceptionHandlerTest} 契約測試集中釘死，故此處不再逐端點重測 404/409 的錯誤格式。
 */
@WebMvcTest(BranchController.class)
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BranchService branchService;

    @MockitoBean
    private com.agony.wmsallocation.security.JwtInterceptor jwtInterceptor;

    private BranchDto stubDto() {
        return BranchDto.builder()
                .branchCode("B001")
                .salesOrgCode("S001")
                .branchName("台北營業所")
                .status(ActiveStatus.ACTIVE)
                .build();
    }

    @BeforeEach
    void setUp() {
        when(jwtInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/branches - 無參數應走 findAll，回 200 與清單")
    void findAll_shouldReturn200WithList() throws Exception {
        when(branchService.findAll()).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].branchCode").value("B001"))
                .andExpect(jsonPath("$[0].branchName").value("台北營業所"));

        verify(branchService).findAll();
    }

    @Test
    @DisplayName("GET /api/branches?activeOnly=true - 應分派到 findAllActive")
    void findAll_withActiveOnly_shouldCallFindAllActive() throws Exception {
        when(branchService.findAllActive()).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/branches").param("activeOnly", "true"))
                .andExpect(status().isOk());

        verify(branchService).findAllActive();
    }

    @Test
    @DisplayName("GET /api/branches/{code} 存在 - 應回傳 200 與營業所資料")
    void findByBranchCode_whenExists_shouldReturn200WithDto() throws Exception {
        when(branchService.findByBranchCode("B001")).thenReturn(stubDto());

        mockMvc.perform(get("/api/branches/B001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchCode").value("B001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/branches - 應回傳 201 與新建營業所")
    void createBranch_shouldReturn201WithCreatedBranch() throws Exception {
        BranchCreateRequest request = new BranchCreateRequest("B001", "S001", "台北營業所", "台北市", "02-1234");
        when(branchService.create(any(BranchCreateRequest.class))).thenReturn(stubDto());

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.branchCode").value("B001"))
                .andExpect(jsonPath("$.branchName").value("台北營業所"));
    }

    @Test
    @DisplayName("POST /api/branches 缺必填 - 端點應掛 @Valid，回 400 + VALIDATION_ERROR")
    void createBranch_whenMissingRequired_shouldReturn400() throws Exception {
        BranchCreateRequest request = new BranchCreateRequest("", "S001", "台北營業所", null, null);

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/branches/{code} - 應回傳 200 與更新後營業所")
    void updateBranch_shouldReturn200WithUpdatedBranch() throws Exception {
        BranchUpdateRequest request = new BranchUpdateRequest("S001", "新北營業所", "新北市", "02-9999");
        when(branchService.update(eq("B001"), any(BranchUpdateRequest.class))).thenReturn(stubDto());

        mockMvc.perform(put("/api/branches/B001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branchCode").value("B001"));
    }

    @Test
    @DisplayName("PUT /api/branches/{code} 缺必填 - 端點應掛 @Valid，回 400 + VALIDATION_ERROR")
    void updateBranch_whenMissingRequired_shouldReturn400() throws Exception {
        BranchUpdateRequest request = new BranchUpdateRequest("S001", "", null, null);

        mockMvc.perform(put("/api/branches/B001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /api/branches/{code} - 應回傳 204 並呼叫 service.delete")
    void deleteBranch_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/branches/B001"))
                .andExpect(status().isNoContent());

        verify(branchService).delete("B001");
    }

}
