package com.sgitu.userservice.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Un compte avec cet email existe déjà : " + email);
    }
}
