package io.github.zxcbecause.expense.storage;

import io.github.zxcbecause.expense.model.Category;
import io.github.zxcbecause.expense.model.Expense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvExpenseRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyListWhenFileDoesNotExist() {
        var repo = new CsvExpenseRepository(tempDir.resolve("missing.csv"));
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    void roundTripPreservesAllFields() {
        var repo = new CsvExpenseRepository(tempDir.resolve("data/expenses.csv"));
        List<Expense> expenses = List.of(
                new Expense(1, LocalDate.of(2026, 9, 1), new BigDecimal("12.50"), Category.FOOD, "Pizza, cola"),
                new Expense(2, LocalDate.of(2026, 9, 2), new BigDecimal("3"), Category.TRANSPORT, "He said \"hi\""));

        repo.saveAll(expenses);

        assertEquals(expenses, repo.findAll());
    }

    @Test
    void writesHeader() throws Exception {
        Path file = tempDir.resolve("expenses.csv");
        new CsvExpenseRepository(file).saveAll(List.of());

        assertEquals(CsvExpenseRepository.HEADER, Files.readAllLines(file).get(0));
    }

    @Test
    void malformedLineProducesHelpfulError() throws Exception {
        Path file = tempDir.resolve("broken.csv");
        Files.writeString(file, CsvExpenseRepository.HEADER + "\nnot,a,valid\n");

        var ex = assertThrows(IllegalStateException.class, () -> new CsvExpenseRepository(file).findAll());
        assertTrue(ex.getMessage().contains("line 2"));
    }
}
