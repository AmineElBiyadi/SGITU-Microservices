package com.sgitu.userservice.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Utilisateur introuvable avec l'id : " + id);
    }

    public UserNotFoundException(String email) {
        super("Utilisateur introuvable avec l'email : " + email);
    }
}
