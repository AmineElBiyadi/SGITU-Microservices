package com.g7suivivehicules.util;

/**
 * Utilitaires pour les calculs géographiques.
 */
public class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
        // Constructeur privé pour empêcher l'instanciation
    }

    /**
     * Calcule la distance entre deux points GPS en mètres en utilisant la formule de Haversine.
     * 
     * @param lat1 Latitude point 1
     * @param lon1 Longitude point 1
     * @param lat2 Latitude point 2
     * @param lon2 Longitude point 2
     * @return Distance en mètres
     */
    public static double calculerDistanceMetres(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000; // Conversion en mètres
    }
}
