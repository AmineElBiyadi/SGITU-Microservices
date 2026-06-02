package ma.sgitu.g8.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaVersionValidatorTest {

    // -------------------------------------------------------------------------
    // A – missing schemaVersion key
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("A – null map → error")
    void nullMap_returnsError() {
        String result = SchemaVersionValidator.validate(null, 1);
        assertThat(result).isNotNull().contains("schemaVersion");
    }

    @Test
    @DisplayName("B – map without schemaVersion key → error mentioning schemaVersion")
    void missingKey_returnsError() {
        Map<String, Object> raw = Map.of("timestamp", "2024-01-01T00:00:00Z");
        String result = SchemaVersionValidator.validate(raw, 1);
        assertThat(result).isNotNull().contains("schemaVersion");
    }

    // -------------------------------------------------------------------------
    // C – non-integer schemaVersion
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("C – schemaVersion is a string that isn't a number → error")
    void nonNumericVersion_returnsError() {
        Map<String, Object> raw = Map.of("schemaVersion", "abc");
        String result = SchemaVersionValidator.validate(raw, 1);
        assertThat(result).isNotNull().contains("integer");
    }

    // -------------------------------------------------------------------------
    // D – wrong version number
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("D – schemaVersion=99 when expected=1 → error mentioning both versions")
    void wrongVersion_returnsError() {
        Map<String, Object> raw = Map.of("schemaVersion", 99);
        String result = SchemaVersionValidator.validate(raw, 1);
        assertThat(result).isNotNull()
                .contains("99")
                .contains("1");
    }

    // -------------------------------------------------------------------------
    // E – correct version
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("E – schemaVersion=1 when expected=1 → no error (null)")
    void correctVersion_returnsNull() {
        Map<String, Object> raw = Map.of("schemaVersion", 1);
        String result = SchemaVersionValidator.validate(raw, 1);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("F – schemaVersion as numeric string \"1\" when expected=1 → accepted")
    void versionAsString_accepted() {
        Map<String, Object> raw = Map.of("schemaVersion", "1");
        String result = SchemaVersionValidator.validate(raw, 1);
        assertThat(result).isNull();
    }

    // -------------------------------------------------------------------------
    // F – null map with null value for key
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("G – schemaVersion key present but value is null → error")
    void nullValueForKey_returnsError() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("schemaVersion", null);
        String result = SchemaVersionValidator.validate(raw, 1);
        assertThat(result).isNotNull().contains("schemaVersion");
    }
}
