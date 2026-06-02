package ma.sgitu.g8.schema;

import java.util.Map;

public final class SchemaVersionValidator {

    private SchemaVersionValidator() {}

    public static String validate(Map<String, Object> raw, int expectedVersion) {
        if (raw == null) {
            return "Missing required field: schemaVersion (expected " + expectedVersion + ").";
        }
        Object versionValue = raw.get("schemaVersion");
        if (versionValue == null) {
            return "Missing required field: schemaVersion (expected " + expectedVersion + ").";
        }
        int version;
        try {
            version = Integer.parseInt(String.valueOf(versionValue));
        } catch (NumberFormatException ex) {
            return "Field schemaVersion must be an integer (expected " + expectedVersion + ").";
        }
        if (version != expectedVersion) {
            return "Unsupported schemaVersion " + version + ": expected " + expectedVersion + ".";
        }
        return null;
    }
}
