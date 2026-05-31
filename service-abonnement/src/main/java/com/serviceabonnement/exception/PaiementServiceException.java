package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class PaiementServiceException extends BaseException {
    public PaiementServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }

    public PaiementServiceException(String message, Throwable cause) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
