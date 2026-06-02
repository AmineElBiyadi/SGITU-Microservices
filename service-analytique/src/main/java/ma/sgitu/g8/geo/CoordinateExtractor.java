package ma.sgitu.g8.geo;

import ma.sgitu.g8.model.IncomingEvent;

import java.util.Map;

public final class CoordinateExtractor {

    private CoordinateExtractor() {}

    public static String resolveZoneLabel(IncomingEvent event) {
        if (event == null) {
            return ZoneResolver.UNKNOWN_ZONE;
        }
        if (event.getZoneId() != null && !event.getZoneId().isBlank()) {
            return event.getZoneId();
        }
        Double lat = readCoordinate(event.getPayload(), "latitude", "lat");
        Double lon = readCoordinate(event.getPayload(), "longitude", "lon", "lng");
        if (lat == null || lon == null) {
            return ZoneResolver.UNKNOWN_ZONE;
        }
        return ZoneResolver.resolve(lat, lon);
    }

    private static Double readCoordinate(Map<String, Object> payload, String... keys) {
        if (payload == null) {
            return null;
        }
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null || String.valueOf(value).isBlank()) {
                continue;
            }
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
