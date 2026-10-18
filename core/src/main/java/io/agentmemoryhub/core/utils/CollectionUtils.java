package io.agentmemoryhub.core.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CollectionUtils {
    private CollectionUtils() {}

    public static Map<String, Object> mapOf(Object... entries) {
        if (Objects.isNull(entries) || entries.length % 2 != 0) {
            throw new IllegalArgumentException("Map entries must be supplied as key-value pairs.");
        }

        var map = new LinkedHashMap<String, Object>();

        for (var index = 0; index < entries.length; index += 2) {
            if (!(entries[index] instanceof String key)) {
                throw new IllegalArgumentException("Map keys must be non-null strings.");
            }

            if (map.containsKey(key)) {
                throw new IllegalArgumentException("Map keys must be unique: " + key);
            }

            if (Objects.isNull(entries[index + 1])) {
                throw new IllegalArgumentException("Map values must be non-null.");
            }

            map.put(key, entries[index + 1]);
        }

        return Collections.unmodifiableMap(map);
    }
}
