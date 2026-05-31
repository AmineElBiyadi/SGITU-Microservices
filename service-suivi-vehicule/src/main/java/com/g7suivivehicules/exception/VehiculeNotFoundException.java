package com.g7suivivehicules.exception;

import java.util.UUID;

/**
 * Exception lancée quand un véhicule n'est pas trouvé en base de données.
 * Remplace jakarta.persistence.EntityNotFoundException pour découpler le code métier de JPA.
 */
public class VehiculeNotFoundException extends BusinessException {
    public VehiculeNotFoundException(UUID id) {
        super("Véhicule introuvable avec l'ID : " + id);
    }
}
