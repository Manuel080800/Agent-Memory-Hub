package io.agentmemoryhub.core.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagsJsonDataConverterTest {
    private final TagsJsonDataConverter converter = new TagsJsonDataConverter(new ObjectMapper());

    @Test
    void serializesAndDeserializesTagsAfterCaseInsensitiveDeduplication() {
        // Verifies tags are trimmed, blank entries removed, and duplicate labels collapsed.
        var json = converter.toJson(Arrays.asList(" Java ", "java", null, " ", "MCP"));

        assertEquals("[\"Java\",\"MCP\"]", json);
        assertEquals(List.of("Java", "MCP"), converter.fromJson(json));
    }

    @Test
    void turnsMissingTagsIntoAnEmptyImmutableListAndRejectsMalformedJson() {
        // Verifies null tags normalize to an empty list and malformed persisted JSON is reported.
        assertEquals("[]", converter.toJson(null));
        var emptyTagsList = converter.fromJson("[]");
        assertEquals(List.of(), emptyTagsList);
        assertThrows(UnsupportedOperationException.class, () -> emptyTagsList.add("tag"));
        assertThrows(IllegalStateException.class, () -> converter.fromJson("{invalid"));
    }
}
