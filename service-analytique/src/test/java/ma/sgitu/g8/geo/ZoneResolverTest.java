package ma.sgitu.g8.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneResolverTest {

    @Test
    void roundsCoordinatesToTwoDecimalPlaces() {
        assertEquals("33.57,-7.59", ZoneResolver.resolve(33.5731, -7.5898));
    }

    @Test
    void handlesNegativeCoordinates() {
        assertEquals("-33.58,-7.59", ZoneResolver.resolve(-33.576, -7.591));
    }

    @Test
    void outputMatchesLatLonFormat() {
        String zone = ZoneResolver.resolve(48.8566, 2.3522);
        assertTrue(zone.matches("^-?\\d+\\.\\d{2},-?\\d+\\.\\d{2}$"));
        assertEquals("48.86,2.35", zone);
    }
}
