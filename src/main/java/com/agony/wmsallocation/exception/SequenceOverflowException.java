package com.agony.wmsallocation.exception;

/**
 * 單日序號溢號（超過 999）。對應 HTTP 409。
 *
 * <p>與 {@link DuplicateResourceException} 不同：溢號只有單一 {@link ErrorCode}，
 * 不需依領域區分，故 code 固定在建構子內，呼叫端只傳 message。
 */
public class SequenceOverflowException extends BusinessException {
    public SequenceOverflowException(String message) {
        super(message, ErrorCode.SEQUENCE_OVERFLOW);
    }
}
