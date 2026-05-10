package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class AbonnementStatutInvalideException extends BaseException {
    public AbonnementStatutInvalideException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
