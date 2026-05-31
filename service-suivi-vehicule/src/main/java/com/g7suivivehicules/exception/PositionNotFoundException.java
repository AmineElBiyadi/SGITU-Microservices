package com.g7suivivehicules.exception;

import java.util.UUID;

/**
 * Exception lancée quand aucune position GPS n'est trouvée pour un véhicule.
 */
public class PositionNotFoundException extends BusinessException {
    public PositionNotFoundException(UUID vehiculeId) {
        super("Aucune position GPS trouvée pour le véhicule : " + vehiculeId);
    }

    public PositionNotFoundException(String message) {
        super(message);
    }
}
