package io.github.zxcbecause.expense.service;

import io.github.zxcbecause.expense.model.Category;
import io.github.zxcbecause.expense.model.Expense;
import io.github.zxcbecause.expense.storage.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseServiceTest {

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(new InMemoryRepository());
    }

    @Test
    void addAssignsIncrementingIds() {
        Expense first = service.add(LocalDate.of(2026, 9, 1), new BigDecimal("10.00"), Category.FOOD, "Coffee");
        Expense second = service.add(LocalDate.of(2026, 9, 2), new BigDecimal("5.00"), Category.TRANSPORT, "Bus");

        assertEquals(1, first.id());
        assertEquals(2, second.id());
    }

    @Test
    void listFiltersByMonthAndCategoryAndSortsNewestFirst() {
        service.add(LocalDate.of(2026, 8, 31), new BigDecimal("7"), Category.FOOD, "August");
        service.add(LocalDate.of(2026, 9, 1), new BigDecimal("10"), Category.FOOD, "Early");
        service.add(LocalDate.of(2026, 9, 20), new BigDecimal("12"), Category.FOOD, "Late");
        service.add(LocalDate.of(2026, 9, 10), new BigDecimal("40"), Category.SHOPPING, "Shoes");

        List<Expense> food = service.list(YearMonth.of(2026, 9), Category.FOOD);

        assertEquals(List.of("Late", "Early"), food.stream().map(Expense::description).toList());
    }

    @Test
    void totalsByCategorySumsAmounts() {
        YearMonth sep = YearMonth.of(2026, 9);
        service.add(sep.atDay(1), new BigDecimal("10.50"), Category.FOOD, "");
        service.add(sep.atDay(2), new BigDecimal("4.50"), Category.FOOD, "");
        service.add(sep.atDay(3), new BigDecimal("20"), Category.HEALTH, "");

        Map<Category, BigDecimal> totals = service.totalsByCategory(sep);

        assertEquals(0, new BigDecimal("15.00").compareTo(totals.get(Category.FOOD)));
        assertEquals(0, new BigDecimal("20").compareTo(totals.get(Category.HEALTH)));
        assertFalse(totals.containsKey(Category.TRANSPORT));
        assertEquals(0, new BigDecimal("35.00").compareTo(service.total(sep)));
    }

    @Test
    void totalsByMonthAreChronological() {
        service.add(LocalDate.of(2026, 9, 1), BigDecimal.ONE, Category.OTHER, "");
        service.add(LocalDate.of(2026, 7, 1), BigDecimal.TEN, Category.OTHER, "");

        assertEquals(List.of(YearMonth.of(2026, 7), YearMonth.of(2026, 9)),
                List.copyOf(service.totalsByMonth().keySet()));
    }

    @Test
    void deleteRemovesExistingExpenseOnly() {
        Expense e = service.add(LocalDate.now(), BigDecimal.ONE, Category.OTHER, "");

        assertFalse(service.delete(999));
        assertTrue(service.delete(e.id()));
        assertTrue(service.findById(e.id()).isEmpty());
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> service.add(LocalDate.now(), BigDecimal.ZERO, Category.FOOD, ""));
    }

    private static class InMemoryRepository implements ExpenseRepository {
        private List<Expense> data = new ArrayList<>();

        @Override
        public List<Expense> findAll() {
            return new ArrayList<>(data);
        }

        @Override
        public void saveAll(List<Expense> expenses) {
            data = new ArrayList<>(expenses);
        }
    }
}
