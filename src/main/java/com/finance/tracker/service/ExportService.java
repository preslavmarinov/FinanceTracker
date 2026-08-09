package com.finance.tracker.service;

import com.finance.tracker.model.Budget;
import com.finance.tracker.model.Transaction;
import com.finance.tracker.model.TransactionType;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Service
public class ExportService {

    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;

    public ExportService(TransactionRepository transactionRepository, BudgetRepository budgetRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
    }

    public byte[] generateMonthlyReport(User user, int year, int month) {
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        String periodLabel = monthLabel(month, year);

        List<Transaction> transactions = transactionRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, from, to);
        List<Budget> budgets = budgetRepository.findByUserOrderByCategoryNameAsc(user);

        StringBuilder sb = new StringBuilder();

        row(sb, "FINANCE TRACKER - MONTHLY REPORT");
        row(sb, "Period", periodLabel);
        row(sb, "Generated", LocalDate.now());
        blank(sb);

        appendSummarySection(sb, periodLabel, transactions);
        blank(sb);

        if (!budgets.isEmpty()) {
            appendMonthlyBudgetSection(sb, user, budgets, from, to);
            blank(sb);
        }

        appendCategorySection(sb, "INCOME BY CATEGORY", transactions, TransactionType.INCOME);
        blank(sb);

        appendCategorySection(sb, "EXPENSE BY CATEGORY", transactions, TransactionType.EXPENSE);
        blank(sb);

        appendTransactionsSection(sb, transactions);

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateYearlyReport(User user, int year) {
        int lastMonth = (year == LocalDate.now().getYear())
                ? LocalDate.now().getMonthValue() : 12;

        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, lastMonth, 1)
                .withDayOfMonth(LocalDate.of(year, lastMonth, 1).lengthOfMonth());

        List<Transaction> allTransactions = transactionRepository
                .findByUserAndDateBetweenOrderByDateAsc(user, from, to);
        List<Budget> budgets = budgetRepository.findByUserOrderByCategoryNameAsc(user);

        StringBuilder sb = new StringBuilder();

        row(sb, "FINANCE TRACKER - YEARLY REPORT");
        row(sb, "Year", year);
        row(sb, "Generated", LocalDate.now());
        blank(sb);

        row(sb, "MONTHLY SUMMARY");
        row(sb, "Month", "Income", "Expense", "Net Balance");
        BigDecimal yearIncome = BigDecimal.ZERO;
        BigDecimal yearExpense = BigDecimal.ZERO;

        for (int m = 1; m <= lastMonth; m++) {
            LocalDate mFrom = LocalDate.of(year, m, 1);
            LocalDate mTo = mFrom.withDayOfMonth(mFrom.lengthOfMonth());
            List<Transaction> monthTx = inRange(allTransactions, mFrom, mTo);
            BigDecimal inc = sum(monthTx, TransactionType.INCOME);
            BigDecimal exp = sum(monthTx, TransactionType.EXPENSE);
            yearIncome = yearIncome.add(inc);
            yearExpense = yearExpense.add(exp);
            row(sb, monthLabel(m, year), inc, exp, inc.subtract(exp));
        }
        row(sb, "TOTAL", yearIncome, yearExpense, yearIncome.subtract(yearExpense));
        blank(sb);

        if (!budgets.isEmpty()) {
            row(sb, "BUDGET OVERVIEW - MONTHLY UTILIZATION");
            List<Object> header = new ArrayList<>();
            header.add("Category");
            header.add("Monthly Limit");
            for (int m = 1; m <= lastMonth; m++) {
                header.add(Month.of(m).getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            }
            row(sb, header.toArray());

            for (Budget budget : budgets) {
                List<Object> budgetRow = new ArrayList<>();
                budgetRow.add(budget.getCategory().getName());
                budgetRow.add(budget.getMonthlyLimit());
                for (int m = 1; m <= lastMonth; m++) {
                    LocalDate mFrom = LocalDate.of(year, m, 1);
                    LocalDate mTo = mFrom.withDayOfMonth(mFrom.lengthOfMonth());
                    BigDecimal spent = transactionRepository.sumExpenseForCategoryAndPeriod(
                            user, budget.getCategory(), mFrom, mTo);
                    budgetRow.add(spent);
                }
                row(sb, budgetRow.toArray());
            }
            blank(sb);
        }

        appendCategorySection(sb, "INCOME BY CATEGORY", allTransactions, TransactionType.INCOME);
        blank(sb);

        appendCategorySection(sb, "EXPENSE BY CATEGORY", allTransactions, TransactionType.EXPENSE);
        blank(sb);

        appendTransactionsSection(sb, allTransactions);

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendSummarySection(StringBuilder sb, String periodLabel, List<Transaction> transactions) {
        BigDecimal income = sum(transactions, TransactionType.INCOME);
        BigDecimal expense = sum(transactions, TransactionType.EXPENSE);
        row(sb, "SUMMARY");
        row(sb, "Period", "Total Income", "Total Expense", "Net Balance");
        row(sb, periodLabel, income, expense, income.subtract(expense));
    }

    private void appendMonthlyBudgetSection(StringBuilder sb, User user, List<Budget> budgets,
                                            LocalDate from, LocalDate to) {
        row(sb, "BUDGET OVERVIEW");
        row(sb, "Category", "Monthly Limit", "Spent", "Remaining", "% Used");
        for (Budget budget : budgets) {
            BigDecimal spent = transactionRepository.sumExpenseForCategoryAndPeriod(
                    user, budget.getCategory(), from, to);
            BigDecimal remaining = budget.getMonthlyLimit().subtract(spent);
            int pct = pct(spent, budget.getMonthlyLimit());
            row(sb, budget.getCategory().getName(),
                    budget.getMonthlyLimit(), spent, remaining, pct + "%");
        }
    }

    private void appendCategorySection(StringBuilder sb, String title,
                                       List<Transaction> transactions, TransactionType type) {
        row(sb, title);
        row(sb, "Category", "Amount");
        byCategory(transactions, type).forEach((cat, amt) -> row(sb, cat, amt));
    }

    private void appendTransactionsSection(StringBuilder sb, List<Transaction> transactions) {
        row(sb, transactions.size() == 1 ? "TRANSACTION" : "TRANSACTIONS");
        row(sb, "Date", "Title", "Category", "Type", "Amount", "Notes");
        for (Transaction t : transactions) {
            row(sb,
                    t.getDate(),
                    t.getTitle(),
                    t.getCategory() != null ? t.getCategory().getName() : "",
                    t.getType().name(),
                    t.getAmount(),
                    t.getNotes() != null ? t.getNotes() : "");
        }
    }

    private void row(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells[i]));
        }
        sb.append("\r\n");
    }

    private void blank(StringBuilder sb) {
        sb.append("\r\n");
    }

    private String escape(Object value) {
        if (value == null) return "";
        if (value instanceof BigDecimal bd) {
            return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private BigDecimal sum(List<Transaction> transactions, TransactionType type) {
        return transactions.stream()
                .filter(t -> t.getType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Transaction> inRange(List<Transaction> all, LocalDate from, LocalDate to) {
        return all.stream()
                .filter(t -> !t.getDate().isBefore(from) && !t.getDate().isAfter(to))
                .toList();
    }

    private Map<String, BigDecimal> byCategory(List<Transaction> transactions, TransactionType type) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (Transaction t : transactions) {
            if (t.getType() == type) {
                String cat = t.getCategory() != null ? t.getCategory().getName() : "(No Category)";
                map.merge(cat, t.getAmount(), BigDecimal::add);
            }
        }
        return map.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
    }

    private int pct(BigDecimal spent, BigDecimal limit) {
        if (limit.compareTo(BigDecimal.ZERO) == 0) return 0;
        return spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 0, RoundingMode.HALF_UP).intValue();
    }

    private String monthLabel(int month, int year) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + year;
    }
}
