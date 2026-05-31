package com.g7suivivehicules.exception;

import java.util.UUID;

/**
 * Exception lancée quand une alerte n'est pas trouvée en base de données.
 */
public class AlertNotFoundException extends BusinessException {
    public AlertNotFoundException(UUID id) {
        super("Alerte introuvable avec l'ID : " + id);
    }
}
