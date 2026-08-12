package com.order.orderprocessing.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST),
    DUPLICATE_PRODUCT(HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND),
    PRODUCT_NOT_ACTIVE(HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),
    ORDER_CANNOT_BE_CANCELLED(HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT),
    PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT),
    ORDER_CANNOT_BE_PAID(HttpStatus.CONFLICT),
    REFUND_ALREADY_COMPLETED(HttpStatus.CONFLICT),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
