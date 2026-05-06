package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AbonnementNotFoundException extends RuntimeException {
    public AbonnementNotFoundException(String message) {
        super(message);
    }
}
