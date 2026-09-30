package io.github.zxcbecause.expense.model;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum Category {
    FOOD,
    TRANSPORT,
    HOUSING,
    ENTERTAINMENT,
    HEALTH,
    EDUCATION,
    SHOPPING,
    OTHER;

    public static Category parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Category must not be empty");
        }
        try {
            return Category.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown category '" + value + "'. Available: " + available());
        }
    }

    public static String available() {
        return Arrays.stream(values())
                .map(c -> c.name().toLowerCase())
                .collect(Collectors.joining(", "));
    }
}
