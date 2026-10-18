package io.agentmemoryhub.core.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TagsJsonDataConverter {

    private final ObjectMapper objectMapper;

    public String toJson(List<String> tagsList) {
        var normalizedTagsList = normalizeTags(tagsList);
        try {
            return objectMapper.writeValueAsString(normalizedTagsList);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Tags could not be serialized.", exception);
        }
    }

    public List<String> fromJson(String tagsJson) {
        try {
            var tagsList = objectMapper.readValue(tagsJson, new TypeReference<List<String>>() { });
            return normalizeTags(tagsList);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored tags contain invalid JSON.", exception);
        }
    }

    private List<String> normalizeTags(List<String> tagsList) {
        if (Objects.isNull(tagsList)) {
            return List.of();
        }

        var normalizedTagsList = new ArrayList<String>();
        var seenNormalizedTags = new HashSet<String>();
        for (var tag : tagsList) {
            if (Objects.isNull(tag) || tag.isBlank()) {
                continue;
            }

            var trimmedTag = tag.strip();
            var comparisonTag = trimmedTag.toLowerCase(Locale.ROOT);
            if (seenNormalizedTags.add(comparisonTag)) {
                normalizedTagsList.add(trimmedTag);
            }
        }

        return List.copyOf(normalizedTagsList);
    }
}
