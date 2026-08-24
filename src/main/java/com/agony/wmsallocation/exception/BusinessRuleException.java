package com.agony.wmsallocation.exception;

/**
 * 一般業務規則違反（不落在重複／下轄資料／找不到等既有形狀）。
 *
 * <p>因違反的規則依情境不同需要不同的 {@link ErrorCode}（例如日期區間、狀態機禁止編輯），
 * 故由呼叫端指定 code，保持本例外可跨領域複用。
 */
public class BusinessRuleException extends BusinessException {
    public BusinessRuleException(String message, ErrorCode errorCode) {
        super(message, errorCode);
    }
}
