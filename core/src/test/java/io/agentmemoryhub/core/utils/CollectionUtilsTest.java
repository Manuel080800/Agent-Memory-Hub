package io.agentmemoryhub.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

    @Test
    void createsAnImmutableOrderedMapFromKeyValuePairs() {
        // Verifies that mapOf preserves entry order and rejects mutation.
        var map = CollectionUtils.mapOf("first", 1, "second", 2);

        assertEquals(List.of("first", "second"), List.copyOf(map.keySet()));
        assertThrows(UnsupportedOperationException.class, () -> map.put("third", 3));
    }

    @Test
    void rejectsMalformedMapEntries() {
        // Verifies that malformed, duplicate, and null map entries are rejected.
        assertThrows(IllegalArgumentException.class, () -> CollectionUtils.mapOf("key"));
        assertThrows(IllegalArgumentException.class, () -> CollectionUtils.mapOf("key", 1, "key", 2));
        assertThrows(IllegalArgumentException.class, () -> CollectionUtils.mapOf("key", null));
    }
}
