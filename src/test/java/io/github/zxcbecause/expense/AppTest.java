package io.github.zxcbecause.expense;

import io.github.zxcbecause.expense.service.ExpenseService;
import io.github.zxcbecause.expense.storage.CsvExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

    @TempDir
    Path tempDir;

    private ByteArrayOutputStream buffer;
    private App app;

    @BeforeEach
    void setUp() {
        buffer = new ByteArrayOutputStream();
        var service = new ExpenseService(new CsvExpenseRepository(tempDir.resolve("e.csv")));
        app = new App(service, new PrintStream(buffer, true, StandardCharsets.UTF_8));
    }

    private String output() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    @Test
    void addThenListShowsExpense() {
        assertEquals(0, app.run(new String[]{"add", "12,5", "food", "Lunch", "--date", "2026-09-15"}));
        assertEquals(0, app.run(new String[]{"list", "--month", "2026-09"}));

        String out = output();
        assertTrue(out.contains("Added #1: 12.50 food on 2026-09-15"));
        assertTrue(out.contains("Lunch"));
        assertTrue(out.contains("Total: 12.50 (1 items)"));
    }

    @Test
    void reportShowsCategoryShares() {
        app.run(new String[]{"add", "75", "housing", "--date", "2026-09-01"});
        app.run(new String[]{"add", "25", "food", "--date", "2026-09-02"});
        app.run(new String[]{"report", "--month", "2026-09"});

        String out = output();
        assertTrue(out.contains("75.0%"));
        assertTrue(out.contains("25.0%"));
        assertTrue(out.contains("100.00"));
    }

    @Test
    void invalidInputReturnsErrorCode() {
        assertEquals(2, app.run(new String[]{"add", "abc", "food"}));
        assertEquals(2, app.run(new String[]{"add", "10", "cars"}));
        assertTrue(output().contains("Unknown category"));
    }

    @Test
    void unknownCommandPrintsHelp() {
        assertEquals(1, app.run(new String[]{"fly"}));
        assertTrue(output().contains("Commands:"));
    }
}
