package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

public class PlanNotFoundException extends BaseException {
    public PlanNotFoundException(Long id) {
        super("Plan non trouvé avec l'ID : " + id, HttpStatus.NOT_FOUND);
    }
}
