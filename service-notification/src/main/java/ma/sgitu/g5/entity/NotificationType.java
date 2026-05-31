package ma.sgitu.g5.entity;

/**
 * Types de notification supportés par G5.
 *
 * EMAIL / SMS / PUSH : notifications utilisateur classiques
 * LOG               : notification de type fichier log — utilisée au niveau admin
 *                     pour tracer les événements critiques (incidents, alertes sécurité, etc.)
 *                     Routée vers LogFileAdapter (écriture dans logs/admin-notifications.log)
 */
public enum NotificationType {
    EMAIL, SMS, PUSH, LOG
}

