package io.github.zxcbecause.expense;

import io.github.zxcbecause.expense.model.Category;
import io.github.zxcbecause.expense.model.Expense;
import io.github.zxcbecause.expense.service.ExpenseService;
import io.github.zxcbecause.expense.storage.CsvExpenseRepository;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point. Usage examples:
 * <pre>
 *   expense add 12.50 food "Lunch with friends"
 *   expense add 30 transport --date 2026-09-01
 *   expense list --month 2026-09 --category food
 *   expense report --month 2026-09
 *   expense delete 3
 * </pre>
 */
public class App {

    private static final String DEFAULT_FILE = "expenses.csv";

    private final ExpenseService service;
    private final PrintStream out;

    public App(ExpenseService service, PrintStream out) {
        this.service = service;
        this.out = out;
    }

    public static void main(String[] args) {
        String file = System.getenv().getOrDefault("EXPENSES_FILE", DEFAULT_FILE);
        App app = new App(new ExpenseService(new CsvExpenseRepository(Path.of(file))), System.out);
        int code = app.run(args);
        System.exit(code);
    }

    public int run(String[] args) {
        if (args.length == 0) {
            printHelp();
            return 1;
        }
        try {
            switch (args[0]) {
                case "add" -> add(args);
                case "list" -> list(args);
                case "delete" -> delete(args);
                case "report" -> report(args);
                case "help", "--help", "-h" -> printHelp();
                default -> {
                    out.println("Unknown command: " + args[0]);
                    printHelp();
                    return 1;
                }
            }
            return 0;
        } catch (IllegalArgumentException | DateTimeParseException e) {
            out.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private void add(String[] args) {
        Parsed p = Parsed.of(args, 1);
        if (p.positional.size() < 2) {
            throw new IllegalArgumentException("Usage: add <amount> <category> [description] [--date YYYY-MM-DD]");
        }
        BigDecimal amount = parseAmount(p.positional.get(0));
        Category category = Category.parse(p.positional.get(1));
        String description = p.positional.size() > 2
                ? String.join(" ", p.positional.subList(2, p.positional.size()))
                : "";
        LocalDate date = p.options.containsKey("date") ? LocalDate.parse(p.options.get("date")) : LocalDate.now();

        Expense e = service.add(date, amount, category, description);
        out.printf("Added #%d: %s %s on %s%n", e.id(), format(e.amount()), e.category().name().toLowerCase(), e.date());
    }

    private void list(String[] args) {
        Parsed p = Parsed.of(args, 1);
        YearMonth month = p.options.containsKey("month") ? YearMonth.parse(p.options.get("month")) : null;
        Category category = p.options.containsKey("category") ? Category.parse(p.options.get("category")) : null;

        List<Expense> expenses = service.list(month, category);
        if (expenses.isEmpty()) {
            out.println("No expenses found.");
            return;
        }
        out.printf("%-5s %-10s %10s  %-13s %s%n", "ID", "DATE", "AMOUNT", "CATEGORY", "DESCRIPTION");
        for (Expense e : expenses) {
            out.printf("%-5d %-10s %10s  %-13s %s%n",
                    e.id(), e.date(), format(e.amount()), e.category().name().toLowerCase(), e.description());
        }
        BigDecimal total = expenses.stream().map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        out.printf("%nTotal: %s (%d items)%n", format(total), expenses.size());
    }

    private void delete(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: delete <id>");
        }
        long id;
        try {
            id = Long.parseLong(args[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Id must be a number: " + args[1]);
        }
        out.println(service.delete(id) ? "Deleted #" + id : "Expense #" + id + " not found");
    }

    private void report(String[] args) {
        Parsed p = Parsed.of(args, 1);
        if (p.options.containsKey("month")) {
            YearMonth month = YearMonth.parse(p.options.get("month"));
            BigDecimal total = service.total(month);
            out.println("Report for " + month);
            if (total.signum() == 0) {
                out.println("No expenses.");
                return;
            }
            service.totalsByCategory(month).entrySet().stream()
                    .sorted(Map.Entry.<Category, BigDecimal>comparingByValue().reversed())
                    .forEach(entry -> {
                        BigDecimal share = entry.getValue().multiply(BigDecimal.valueOf(100))
                                .divide(total, 1, RoundingMode.HALF_UP);
                        out.printf("  %-13s %10s  %5s%%  %s%n",
                                entry.getKey().name().toLowerCase(), format(entry.getValue()), share, bar(share));
                    });
            out.printf("  %-13s %10s%n", "TOTAL", format(total));
        } else {
            out.println("Monthly totals");
            service.totalsByMonth().forEach((m, sum) -> out.printf("  %s %10s%n", m, format(sum)));
        }
    }

    private void printHelp() {
        out.println("""
                Expense Tracker CLI

                Commands:
                  add <amount> <category> [description] [--date YYYY-MM-DD]
                  list [--month YYYY-MM] [--category NAME]
                  report [--month YYYY-MM]
                  delete <id>
                  help

                Categories: %s
                Data file: $EXPENSES_FILE (default: expenses.csv)""".formatted(Category.available()));
    }

    private static BigDecimal parseAmount(String raw) {
        try {
            BigDecimal amount = new BigDecimal(raw.replace(',', '.'));
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero");
            }
            return amount.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: " + raw);
        }
    }

    private static String format(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String bar(BigDecimal percent) {
        return "#".repeat(percent.divide(BigDecimal.valueOf(5), 0, RoundingMode.HALF_UP).intValue());
    }

    /** Minimal argument parser: positional args plus "--key value" options. */
    private record Parsed(List<String> positional, Map<String, String> options) {
        static Parsed of(String[] args, int from) {
            List<String> positional = new java.util.ArrayList<>();
            Map<String, String> options = new HashMap<>();
            for (int i = from; i < args.length; i++) {
                if (args[i].startsWith("--")) {
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + args[i]);
                    }
                    options.put(args[i].substring(2), args[++i]);
                } else {
                    positional.add(args[i]);
                }
            }
            return new Parsed(positional, options);
        }
    }
}
