package io.github.zxcbecause.expense.storage;

import io.github.zxcbecause.expense.model.Expense;

import java.util.List;

public interface ExpenseRepository {

    List<Expense> findAll();

    void saveAll(List<Expense> expenses);
}
