package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.dto.master.ProductCreateRequest;
import com.agony.wmsallocation.dto.master.ProductDto;
import com.agony.wmsallocation.dto.master.ProductUpdateRequest;
import com.agony.wmsallocation.entity.enums.ActiveStatus;
import com.agony.wmsallocation.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ProductController} 的 Web 層測試。
 *
 * <p>確保 Web 層能正確分派路由、處理 activeOnly 參數，
 * 並對新增與修改的請求物件掛上 @Valid 進行驗證。
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private com.agony.wmsallocation.security.JwtInterceptor jwtInterceptor;

    private ProductDto stubDto() {
        return ProductDto.builder()
                .productCode("P001")
                .productName("測試商品")
                .baseUnit("瓶")
                .basePrice(new BigDecimal("50.00"))
                .status(ActiveStatus.ACTIVE)
                .build();
    }

    @BeforeEach
    void setUp() {
        when(jwtInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("GET /api/products - 預設應呼叫 findAll 並回傳 200 與清單")
    void findAll_shouldReturn200WithList() throws Exception {
        when(productService.findAll()).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("P001"))
                .andExpect(jsonPath("$[0].productName").value("測試商品"));

        verify(productService).findAll();
    }

    @Test
    @DisplayName("GET /api/products?activeOnly=true - 應呼叫 findAllActive")
    void findAll_withActiveOnly_shouldCallFindAllActive() throws Exception {
        when(productService.findAllActive()).thenReturn(List.of(stubDto()));

        mockMvc.perform(get("/api/products").param("activeOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productCode").value("P001"));

        verify(productService).findAllActive();
    }

    @Test
    @DisplayName("GET /api/products/{code} - 存在時應回傳 200 與 DTO")
    void findByProductCode_whenExists_shouldReturn200WithDto() throws Exception {
        when(productService.findByProductCode("P001")).thenReturn(stubDto());

        mockMvc.perform(get("/api/products/P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("P001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/products - 應回傳 201 與新建商品")
    void createProduct_shouldReturn201WithCreatedProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "P001", "測試商品", "瓶", new BigDecimal("50.00"));
        when(productService.create(any(ProductCreateRequest.class))).thenReturn(stubDto());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").value("P001"));
    }

    @Test
    @DisplayName("POST /api/products 缺必填欄位 - 應掛 @Valid 回傳 400 + VALIDATION_ERROR")
    void createProduct_whenMissingRequired_shouldReturn400() throws Exception {
        // productName 為空字串
        ProductCreateRequest request = new ProductCreateRequest(
                "P001", "", "瓶", new BigDecimal("50.00"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("PUT /api/products/{code} - 應回傳 200 與更新後商品")
    void updateProduct_shouldReturn200WithUpdatedProduct() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "測試商品更新", "瓶", new BigDecimal("60.00"));
        when(productService.update(eq("P001"), any(ProductUpdateRequest.class))).thenReturn(stubDto());

        mockMvc.perform(put("/api/products/P001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("P001"));
    }

    @Test
    @DisplayName("PUT /api/products/{code} 缺必填欄位 - 應掛 @Valid 回傳 400 + VALIDATION_ERROR")
    void updateProduct_whenMissingRequired_shouldReturn400() throws Exception {
        // basePrice 為 null
        ProductUpdateRequest request = new ProductUpdateRequest(
                "測試商品更新", "瓶", null);

        mockMvc.perform(put("/api/products/P001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("DELETE /api/products/{code} - 應回傳 204 並呼叫 service.delete")
    void deleteProduct_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/products/P001"))
                .andExpect(status().isNoContent());

        verify(productService).delete("P001");
    }
}
