package io.agentmemoryhub.core.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ValidateUtilsTest {

    @Test
    void validatesStringsAndOpaqueIdentifiers() {
        // Verifies that blank identifiers are rejected without throwing for null values.
        assertTrue(ValidateUtils.isValidId("project-1"));
        assertTrue(ValidateUtils.isInvalidString(null));
        assertTrue(ValidateUtils.isInvalidId("   "));
    }

    @Test
    void comparesListElementsByExtractedValueNullSafely() {
        // Verifies extractor-based membership while ignoring null list entries and null extractors.
        var itemList = Arrays.asList(null, "alpha", "beta");
        assertTrue(ValidateUtils.containsInList(itemList, "zeta", String::length));
        assertFalse(ValidateUtils.containsInList(itemList, "longer", String::length));
        assertFalse(ValidateUtils.containsInList(itemList, "alpha", null));
    }

    @Test
    void validatesListsMapsAndSharedExtractedValues() {
        // Verifies empty collections, valid list-valued maps, and same-value stream checks.
        assertTrue(ValidateUtils.isInvalidList(List.of()));
        assertTrue(ValidateUtils.isInvalidList(null));
        assertTrue(ValidateUtils.isInvalidMap(null));
        assertTrue(ValidateUtils.isValidMapWithValidListValues(Map.of("key", List.of("value"))));
        assertFalse(ValidateUtils.isValidMapWithValidListValues(Map.of("key", List.of())));
        assertFalse(ValidateUtils.isValidMapWithValidListValues(Collections.singletonMap("key", null)));
        assertTrue(ValidateUtils.allHaveSameValueByListAndExtractor(List.of("one", "two"), String::length));
    }

    @Test
    void validatesNumbersArraysAndDateRanges() {
        // Verifies numeric comparisons, array presence, and inclusive date-range ordering.
        assertTrue(ValidateUtils.isPositiveBigDecimal(new BigDecimal("1.01")));
        assertTrue(ValidateUtils.isNullOrZero(BigDecimal.ZERO));
        assertTrue(ValidateUtils.isValidArray(new String[]{"value"}));
        assertTrue(ValidateUtils.isValidDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)));
        assertTrue(ValidateUtils.isInvalidDateRange(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)));
    }
}
