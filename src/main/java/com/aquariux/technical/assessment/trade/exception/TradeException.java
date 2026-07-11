package com.aquariux.technical.assessment.trade.exception;

import org.springframework.http.HttpStatus;

public class TradeException extends RuntimeException {
    private final HttpStatus status;

    public TradeException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
