package com.agony.wmsallocation.controller;

import com.agony.wmsallocation.security.JwtInterceptor;
import com.agony.wmsallocation.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link InventoryController} 的 Web 層測試。
 *
 * <p>只釘手動快照這一支的 status code 契約：ADR-0003 不用訊息信封，建立類動作回
 * 201 + Location（指向 GET /snapshot/{date}），body 留空。查詢端點是直通 Service 的
 * 薄轉呼叫，依 backend.md「挑代表性的寫」不重複測。
 */
@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtInterceptor jwtInterceptor;

    @BeforeEach
    void setUp() {
        when(jwtInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Test
    @DisplayName("POST /api/inventory/snapshot - 應回 201 + Location 指向該日快照，body 留空")
    void createSnapshot_shouldReturn201WithLocation() throws Exception {
        when(inventoryService.createTodaySnapshot()).thenReturn(LocalDate.of(2026, 7, 15));

        mockMvc.perform(post("/api/inventory/snapshot"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/inventory/snapshot/2026-07-15"))
                .andExpect(content().string(""));

        verify(inventoryService).createTodaySnapshot();
    }
}
