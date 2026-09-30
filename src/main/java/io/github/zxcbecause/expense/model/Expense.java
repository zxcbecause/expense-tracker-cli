package io.github.zxcbecause.expense.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A single expense entry. Immutable.
 */
public record Expense(long id, LocalDate date, BigDecimal amount, Category category, String description) {

    public Expense {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(category, "category");
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        description = description == null ? "" : description.strip();
    }
}
