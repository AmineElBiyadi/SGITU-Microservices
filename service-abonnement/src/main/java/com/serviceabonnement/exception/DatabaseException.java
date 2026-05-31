package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class DatabaseException extends BaseException {
    public DatabaseException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
