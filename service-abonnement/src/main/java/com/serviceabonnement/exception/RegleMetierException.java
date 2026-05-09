package com.serviceabonnement.exception;

import org.springframework.http.HttpStatus;

/**
 * Lancée quand une règle métier est violée.
 * Exemple : durée de désactivation dépasse le maximum autorisé par le plan.
 */
public class RegleMetierException extends BaseException {
    public RegleMetierException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
