# Expense Tracker CLI

![Java](https://img.shields.io/badge/Java-21-orange) ![Build](https://img.shields.io/badge/build-Maven-blue) ![Tests](https://img.shields.io/badge/tests-JUnit%205-green)

A small command-line app for tracking personal expenses, written in plain Java 21 with no runtime dependencies.
Data is stored in a local CSV file, so it is easy to open in Excel or Google Sheets.

## Features

- Add, list and delete expenses
- Filter by month and category
- Monthly report with category breakdown, percentages and a text bar chart
- Totals per month
- Safe persistence: writes go to a temp file first and are then moved atomically
- Money is handled with `BigDecimal`, never `double`

## Example

```text
$ java -jar target/expense-tracker-cli-1.0.0.jar add 12.50 food "Pizza, cola" --date 2026-09-01
Added #1: 12.50 food on 2026-09-01

$ java -jar target/expense-tracker-cli-1.0.0.jar list
ID    DATE           AMOUNT  CATEGORY      DESCRIPTION
2     2026-09-03      75.00  housing
1     2026-09-01      12.50  food          Pizza, cola
3     2026-08-20      30.00  transport     Taxi

Total: 117.50 (3 items)

$ java -jar target/expense-tracker-cli-1.0.0.jar report --month 2026-09
Report for 2026-09
  housing            75.00   85.7%  #################
  food               12.50   14.3%  ###
  TOTAL              87.50
```

## Getting started

Requirements: JDK 21+, Maven 3.9+.

```bash
mvn package
java -jar target/expense-tracker-cli-1.0.0.jar help
```

By default data is saved to `expenses.csv` in the current directory. Use the `EXPENSES_FILE` environment variable to change it.

## Commands

| Command | Description |
|---|---|
| `add <amount> <category> [description] [--date YYYY-MM-DD]` | Add an expense (date defaults to today) |
| `list [--month YYYY-MM] [--category NAME]` | Show expenses, newest first |
| `report [--month YYYY-MM]` | Category breakdown for a month, or totals per month |
| `delete <id>` | Remove an expense |

Categories: `food`, `transport`, `housing`, `entertainment`, `health`, `education`, `shopping`, `other`.

## Project structure

```text
src/main/java/io/github/zxcbecause/expense
├── App.java                      # CLI parsing and output
├── model/                        # Expense record, Category enum
├── service/ExpenseService.java   # business logic: filtering, aggregation
└── storage/                      # repository interface + CSV implementation
```

The storage layer sits behind an `ExpenseRepository` interface, so the service is tested with an in-memory fake and the CSV format could be swapped for a database later.

## Tests

```bash
mvn test
```

Unit tests cover the service logic, CSV round-trips (including commas and quotes in descriptions), error handling and the CLI commands end to end.

## Ideas for next steps

- Budgets per category with warnings
- Export report to Markdown/HTML
- SQLite storage
