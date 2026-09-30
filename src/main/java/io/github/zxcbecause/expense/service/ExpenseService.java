package io.github.zxcbecause.expense.service;

import io.github.zxcbecause.expense.model.Category;
import io.github.zxcbecause.expense.model.Expense;
import io.github.zxcbecause.expense.storage.ExpenseRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public Expense add(LocalDate date, BigDecimal amount, Category category, String description) {
        List<Expense> all = new ArrayList<>(repository.findAll());
        long nextId = all.stream().mapToLong(Expense::id).max().orElse(0) + 1;
        Expense expense = new Expense(nextId, date, amount, category, description);
        all.add(expense);
        repository.saveAll(all);
        return expense;
    }

    public boolean delete(long id) {
        List<Expense> all = new ArrayList<>(repository.findAll());
        boolean removed = all.removeIf(e -> e.id() == id);
        if (removed) {
            repository.saveAll(all);
        }
        return removed;
    }

    public Optional<Expense> findById(long id) {
        return repository.findAll().stream().filter(e -> e.id() == id).findFirst();
    }

    /** All expenses, newest first. Optional filters may be {@code null}. */
    public List<Expense> list(YearMonth month, Category category) {
        return repository.findAll().stream()
                .filter(e -> month == null || YearMonth.from(e.date()).equals(month))
                .filter(e -> category == null || e.category() == category)
                .sorted(Comparator.comparing(Expense::date).reversed().thenComparing(Expense::id))
                .toList();
    }

    public BigDecimal total(YearMonth month) {
        return list(month, null).stream()
                .map(Expense::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sum per category, only categories with spending are included. */
    public Map<Category, BigDecimal> totalsByCategory(YearMonth month) {
        return list(month, null).stream()
                .collect(Collectors.groupingBy(
                        Expense::category,
                        () -> new EnumMap<>(Category.class),
                        Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)));
    }

    /** Sum per month, sorted chronologically. */
    public Map<YearMonth, BigDecimal> totalsByMonth() {
        return repository.findAll().stream()
                .collect(Collectors.groupingBy(
                        e -> YearMonth.from(e.date()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::amount, BigDecimal::add)));
    }
}
