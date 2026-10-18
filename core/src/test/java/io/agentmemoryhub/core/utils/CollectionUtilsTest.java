package io.agentmemoryhub.core.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
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

    @Test
    void checksNullAndEmptyCollectionsAndMaps() {
        // Verifies that collection and map emptiness checks handle null and empty values.
        assertTrue(CollectionUtils.isEmpty((List<?>) null));
        assertTrue(CollectionUtils.isEmpty(List.of()));
        assertFalse(CollectionUtils.isEmpty(List.of("value")));
        assertTrue(CollectionUtils.isEmpty((Map<?, ?>) null));
        assertTrue(CollectionUtils.isEmpty(Map.of()));
        assertFalse(CollectionUtils.isEmpty(Map.of("key", "value")));
    }
}
