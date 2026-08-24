package com.agony.wmsallocation.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    AUTH_BAD_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    BRANCH_CODE_DUPLICATED(HttpStatus.CONFLICT),
    BRANCH_HAS_DEPENDENTS(HttpStatus.CONFLICT),
    SALES_ORG_CODE_DUPLICATED(HttpStatus.CONFLICT),
    SALES_ORG_HAS_DEPENDENTS(HttpStatus.CONFLICT),
    CUSTOMER_CODE_DUPLICATED(HttpStatus.CONFLICT),
    FACTORY_CODE_DUPLICATED(HttpStatus.CONFLICT),
    PRODUCT_CODE_DUPLICATED(HttpStatus.CONFLICT),
    SEQUENCE_OVERFLOW(HttpStatus.CONFLICT),
    PURCHASE_DATE_OUT_OF_RANGE(HttpStatus.BAD_REQUEST),
    PURCHASE_ORDER_NOT_EDITABLE(HttpStatus.CONFLICT),
    PRODUCT_FACTORY_NOT_CONFIGURED(HttpStatus.CONFLICT),
    PURCHASE_ORDER_NOT_FOUND(HttpStatus.CONFLICT),
    FDO_ALREADY_SHIPPED(HttpStatus.CONFLICT),
    FDO_NOT_RECEIVABLE(HttpStatus.CONFLICT),
    INVENTORY_INSUFFICIENT(HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    @Getter
    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}