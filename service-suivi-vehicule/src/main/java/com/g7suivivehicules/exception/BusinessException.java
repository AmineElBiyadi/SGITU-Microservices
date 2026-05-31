package com.g7suivivehicules.exception;

/**
 * Exception de base pour toutes les exceptions métier du service G7.
 * Hérite de RuntimeException pour ne pas forcer les blocs try-catch sur les appelants.
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
