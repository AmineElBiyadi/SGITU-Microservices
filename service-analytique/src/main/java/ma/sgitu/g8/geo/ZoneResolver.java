package ma.sgitu.g8.geo;

import java.util.Locale;

public final class ZoneResolver {

    public static final String UNKNOWN_ZONE = "unknown";

    private ZoneResolver() {}

    public static String resolve(double lat, double lon) {
        return String.format(Locale.US, "%.2f,%.2f", round2(lat), round2(lon));
    }

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
