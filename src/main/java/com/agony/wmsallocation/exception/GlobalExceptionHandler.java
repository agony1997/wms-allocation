package com.agony.wmsallocation.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(value = BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn(ex.getMessage(), ex);
        return ResponseEntity.status(ex.getErrorCode().getHttpStatus())
                .body(new ErrorResponse(ex.getErrorCode().getHttpStatus().value(),
                        ex.getMessage(),
                        ex.getErrorCode().name(),
                        LocalDateTime.now(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        log.warn("欄位驗證失敗：{}", message);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.getHttpStatus().value(),
                        message,
                        ErrorCode.VALIDATION_ERROR.name(),
                        LocalDateTime.now(),
                        request.getRequestURI()));
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + "：" + fieldError.getDefaultMessage();
    }

    // 少給必要的 @RequestParam / 型別不合（例如 purchaseDate=abc）；發生在 controller 方法
    // 本體執行前的參數綁定階段，若不處理會落入下方 Exception catch-all 誤回 500。
    // @ExceptionHandler 依 Throwable 子類型比對，故仍須逐一列舉（無法用介面一次涵蓋）。
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        String message = "缺少必要參數：" + ex.getParameterName();
        log.warn(message);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.getHttpStatus().value(),
                        message,
                        ErrorCode.VALIDATION_ERROR.name(),
                        LocalDateTime.now(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "參數格式錯誤：" + ex.getName();
        log.warn(message);
        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(new ErrorResponse(ErrorCode.VALIDATION_ERROR.getHttpStatus().value(),
                        message,
                        ErrorCode.VALIDATION_ERROR.name(),
                        LocalDateTime.now(),
                        request.getRequestURI()));
    }

    // 打到不存在的路徑時，Spring 的資源處理器（對應 /** 的 ResourceHttpRequestHandler）會丟
    // NoResourceFoundException。它是 Exception 子類，若不在此攔下就會落入下方 catch-all 被誤回 500；
    // 找不到路徑語意上就是 404，沿用既有的 RESOURCE_NOT_FOUND。
    @ExceptionHandler(value = NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("找不到資源：{}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(),
                        "找不到請求的資源",
                        ErrorCode.RESOURCE_NOT_FOUND.name(),
                        LocalDateTime.now(),
                        request.getRequestURI()));
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception, HttpServletRequest request) {
        log.error("未預期錯誤", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500,
                        "未預期錯誤",
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        LocalDateTime.now(),
                        request.getRequestURI()));
    }

}
