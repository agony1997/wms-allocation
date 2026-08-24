package com.agony.wmsallocation.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GlobalExceptionHandler} 的契約測試。
 *
 * <p>釘死「例外 → HTTP status → ErrorResponse body」這段跨檔案、由框架仲介、
 * 讀 code 看不出來的對映。此契約一旦回歸（例如有人改了 status 或洩漏內部訊息），
 * 這支會紅；各領域 Controller 就不必每支都重測同一套錯誤格式。
 *
 * <p>用 standalone MockMvc + 一個只為測試存在的 dummy controller：
 * 真正走 Spring MVC 的例外解析路徑（挑選 {@code @ExceptionHandler}、序列化 body），
 * 又不需啟動完整 context 或資料庫，跑得快。
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // ErrorResponse 內含 LocalDateTime，需 JavaTimeModule 才能序列化
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(new ObjectMapper().registerModule(new JavaTimeModule()));

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("ResourceNotFoundException → 404 + RESOURCE_NOT_FOUND，body 形狀正確")
    void resourceNotFound_shouldMapTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatusCode").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("找不到測試資源"))
                .andExpect(jsonPath("$.path").value("/test/not-found"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("DuplicateResourceException → 用呼叫端指定的 ErrorCode 決定 status（409 + BRANCH_CODE_DUPLICATED）")
    void duplicateResource_shouldMapToCallerSpecifiedCode() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.httpStatusCode").value(409))
                .andExpect(jsonPath("$.errorCode").value("BRANCH_CODE_DUPLICATED"))
                .andExpect(jsonPath("$.path").value("/test/duplicate"));
    }

    @Test
    @DisplayName("欄位驗證失敗（MethodArgumentNotValidException）→ 400 + VALIDATION_ERROR，訊息含欄位名")
    void validationError_shouldMapTo400() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))   // name 缺值，觸發 @NotBlank
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message", containsString("name")));
    }

    @Test
    @DisplayName("缺少必要 @RequestParam（框架綁定例外）→ 400 + VALIDATION_ERROR，不落入 500 catch-all")
    void missingRequestParam_shouldMapTo400NotInternalServerError() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatusCode").value(400))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("打到不存在的路徑（NoResourceFoundException）→ 404 + RESOURCE_NOT_FOUND，不落入 500 catch-all")
    void noResourceFound_shouldMapTo404NotInternalServerError() throws Exception {
        mockMvc.perform(get("/test/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.httpStatusCode").value(404))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("未預期例外 → 500 + INTERNAL_SERVER_ERROR，且不洩漏內部訊息")
    void unexpectedException_shouldMapTo500AndHideDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.httpStatusCode").value(500))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("未預期錯誤"))
                // 安全契約：原始例外訊息不可外洩給呼叫端
                .andExpect(jsonPath("$.message", not(containsString("不該外洩"))));
    }

    /**
     * 只為這支測試存在的假 controller，各端點刻意丟出對應例外，
     * 讓 GlobalExceptionHandler 在真實的 MVC 例外解析流程中被觸發。
     */
    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("找不到測試資源");
        }

        @GetMapping("/test/duplicate")
        void duplicate() {
            throw new DuplicateResourceException("營業所代碼已存在", ErrorCode.BRANCH_CODE_DUPLICATED);
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("不該外洩的內部錯誤細節");
        }

        // standalone MockMvc 沒有資源處理鏈，無法自然觸發 NoResourceFoundException，
        // 故直接丟出以釘住「此例外 → 404」的 handler 契約（真實情境是打到未對應的路徑）。
        @GetMapping("/test/no-resource")
        void noResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/test/no-resource");
        }

        @GetMapping("/test/missing-param")
        void missingParam(@RequestParam String id) {
            // 進不來；缺少必要參數會在進入方法前就丟 MissingServletRequestParameterException
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody Payload payload) {
            // 進不來；驗證失敗會在進入方法前就丟 MethodArgumentNotValidException
        }

        record Payload(@NotBlank String name) {
        }
    }
}
