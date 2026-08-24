package com.agony.wmsallocation.exception;

/**
 * 資源仍被引用／尚有下轄資料，無法刪除。對應 HTTP 409。
 *
 * <p>因「下轄關聯」依領域不同需要不同的 {@link ErrorCode}（例如營業所用
 * {@code BRANCH_HAS_DEPENDENTS}），故由呼叫端指定 code，保持本例外可跨領域複用。
 */
public class ResourceInUseException extends BusinessException {
    public ResourceInUseException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
