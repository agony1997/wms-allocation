package com.agony.wmsallocation.exception;

public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message, ErrorCode.RESOURCE_NOT_FOUND);
        // todo 之後收斂訊息格式，未想好格式
    }
}
