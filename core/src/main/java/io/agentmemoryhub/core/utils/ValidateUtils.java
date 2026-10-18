package io.agentmemoryhub.core.utils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class ValidateUtils {

    private ValidateUtils() {
    }

    public static boolean isValidId(String id) {
        return isValidString(id);
    }

    public static boolean isValidId(Long id) {
        return Objects.nonNull(id) && id > 0;
    }

    public static boolean isInvalidId(String id) {
        return !isValidId(id);
    }

    public static boolean isInvalidId(Long id) {
        return !isValidId(id);
    }

    public static <T> boolean isValidList(List<T> list) {
        return !CollectionUtils.isEmpty(list);
    }

    public static <T> boolean isInvalidList(List<T> list) {
        return !isValidList(list);
    }

    public static <T> boolean containsInList(List<T> list, T element) {
        return Objects.nonNull(element) && isValidList(list) && list.contains(element);
    }

    public static <T, R> boolean containsInList(List<T> list, T element, Function<T, R> extractor) {
        if (Objects.isNull(element) || Objects.isNull(extractor) || isInvalidList(list)) {
            return false;
        }

        var expectedValue = extractor.apply(element);
        return list.stream().filter(Objects::nonNull).map(extractor).anyMatch(value -> Objects.equals(expectedValue, value));
    }

    public static <T> boolean notContainsInList(List<T> list, T element) {
        return !containsInList(list, element);
    }

    public static <T, R> boolean notContainsInList(List<T> list, T element, Function<T, R> extractor) {
        return !containsInList(list, element, extractor);
    }

    public static <K, V> boolean isValidMap(Map<K, V> map) {
        return !CollectionUtils.isEmpty(map);
    }

    public static <K, V> boolean isInvalidMap(Map<K, V> map) {
        return !isValidMap(map);
    }

    public static <K, V> boolean isValidMapWithValidListValues(Map<K, List<V>> map) {
        return isValidMap(map) && map.values().stream().noneMatch(CollectionUtils::isEmpty);
    }

    public static <K, V> boolean isInvalidMapWithValidListValues(Map<K, List<V>> map) {
        return !isValidMapWithValidListValues(map);
    }

    public static boolean isNullOrZero(BigDecimal value) {
        return Objects.isNull(value) || BigDecimal.ZERO.compareTo(value) == 0;
    }

    public static boolean isNotNullOrZero(BigDecimal value) {
        return !isNullOrZero(value);
    }

    public static boolean isPositiveBigDecimal(BigDecimal value) {
        return Objects.nonNull(value) && value.compareTo(BigDecimal.ZERO) > 0;
    }

    public static boolean isNegativeBigDecimal(BigDecimal value) {
        return Objects.nonNull(value) && value.compareTo(BigDecimal.ZERO) < 0;
    }

    public static boolean isValidString(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }

    public static boolean isInvalidString(String value) {
        return !isValidString(value);
    }

    public static <T> T defaultIfNull(T value, T defaultValue) {
        return Objects.nonNull(value) ? value : defaultValue;
    }

    public static <T> boolean isValidArray(T[] array) {
        return Objects.nonNull(array) && array.length > 0;
    }

    public static <T> boolean isInvalidArray(T[] array) {
        return !isValidArray(array);
    }

    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (Objects.isNull(startDate) || Objects.isNull(endDate)) {
            return true;
        }

        return !startDate.isAfter(endDate);
    }

    public static boolean isInvalidDateRange(LocalDate startDate, LocalDate endDate) {
        return !isValidDateRange(startDate, endDate);
    }

    public static <T, R> boolean allHaveSameValueByListAndExtractor(List<T> list, Function<T, R> extractor) {
        if (isInvalidList(list) || Objects.isNull(extractor)) {
            return false;
        }

        return list.stream().map(extractor).distinct().count() <= 1;
    }
}
