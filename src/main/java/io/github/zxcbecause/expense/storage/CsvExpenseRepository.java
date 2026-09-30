package io.github.zxcbecause.expense.storage;

import io.github.zxcbecause.expense.model.Category;
import io.github.zxcbecause.expense.model.Expense;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores expenses in a simple CSV file: id,date,amount,category,description.
 * Descriptions are quoted so they may contain commas and quotes.
 */
public class CsvExpenseRepository implements ExpenseRepository {

    static final String HEADER = "id,date,amount,category,description";

    private final Path file;

    public CsvExpenseRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Expense> findAll() {
        if (Files.notExists(file)) {
            return new ArrayList<>();
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            List<Expense> result = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.isBlank() || (i == 0 && line.equals(HEADER))) {
                    continue;
                }
                result.add(parseLine(line, i + 1));
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + file, e);
        }
    }

    @Override
    public void saveAll(List<Expense> expenses) {
        List<String> lines = new ArrayList<>(expenses.size() + 1);
        lines.add(HEADER);
        for (Expense e : expenses) {
            lines.add(String.join(",",
                    String.valueOf(e.id()),
                    e.date().toString(),
                    e.amount().toPlainString(),
                    e.category().name(),
                    quote(e.description())));
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // write to a temp file first so a crash never leaves a half-written file
            Path tmp = Files.createTempFile(parent, "expenses", ".tmp");
            Files.write(tmp, lines, StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write " + file, e);
        }
    }

    private static Expense parseLine(String line, int lineNumber) {
        String[] parts = line.split(",", 5);
        if (parts.length != 5) {
            throw new IllegalStateException("Malformed CSV at line " + lineNumber + ": " + line);
        }
        try {
            return new Expense(
                    Long.parseLong(parts[0]),
                    LocalDate.parse(parts[1]),
                    new BigDecimal(parts[2]),
                    Category.valueOf(parts[3]),
                    unquote(parts[4]));
        } catch (RuntimeException e) {
            throw new IllegalStateException("Malformed CSV at line " + lineNumber + ": " + e.getMessage(), e);
        }
    }

    static String quote(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1).replace("\"\"", "\"");
        }
        return value;
    }
}
