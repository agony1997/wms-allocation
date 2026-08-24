package com.agony.wmsallocation.exception;

/**
 * 資源重複（唯一性衝突）。對應 HTTP 409。
 *
 * <p>因「重複」依領域不同需要不同的 {@link ErrorCode}（例如營業所用
 * {@code BRANCH_CODE_DUPLICATED}），故由呼叫端指定 code，保持本例外可跨領域複用。
 */
public class DuplicateResourceException extends BusinessException {
    public DuplicateResourceException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
