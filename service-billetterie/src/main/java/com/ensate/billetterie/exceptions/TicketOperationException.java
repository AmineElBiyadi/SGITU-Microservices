package com.ensate.billetterie.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class TicketOperationException extends RuntimeException {
    public TicketOperationException(String message) {
        super(message);
    }
}
