package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class TimeoutException extends BaseException {
    public TimeoutException(String message) {
        super(message, HttpStatus.GATEWAY_TIMEOUT);
    }
}
