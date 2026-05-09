package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class UtilisateurNotFoundException extends BaseException {
    public UtilisateurNotFoundException(Long id) {
        super("Utilisateur non trouvé avec l'ID : " + id, HttpStatus.NOT_FOUND);
    }
}
