package com.ensate.billetterie.ticket.utils;

public class Utils {

    public static <T> T assertNotNull(Object value, Class<T> type) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Value cannot be null. Expected type: " + type.getSimpleName()
            );
        }

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Value must be of type " + type.getSimpleName()
            );
        }

        return type.cast(value);
    }
}
