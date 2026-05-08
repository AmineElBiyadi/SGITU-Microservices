package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class AbonnementNotFoundException extends BaseException {
    public AbonnementNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
