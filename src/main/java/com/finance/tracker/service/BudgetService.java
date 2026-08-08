package com.finance.tracker.service;

import com.finance.tracker.model.Budget;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.BudgetRepository;
import com.finance.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public BudgetService(BudgetRepository budgetRepository,
                         TransactionRepository transactionRepository) {
        this.budgetRepository = budgetRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Budget> findAll(User user) {
        return budgetRepository.findByUserOrderByCategoryNameAsc(user);
    }

    public Budget findById(Long id, User user) {
        return budgetRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Budget not found"));
    }

    @Transactional
    public Budget save(Budget budget, User user) {
        boolean isDuplicate = budgetRepository.existsByCategoryIdAndUser(
                budget.getCategory().getId(), user);
        if (isDuplicate && budget.getId() == null) {
            throw new IllegalArgumentException("A budget for this category already exists");
        }
        budget.setUser(user);
        return budgetRepository.save(budget);
    }

    @Transactional
    public void delete(Long id, User user) {
        Budget budget = findById(id, user);
        budgetRepository.delete(budget);
    }

    public List<BudgetStatus> getBudgetStatuses(User user) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        return findAll(user).stream()
                .map(budget -> {
                    BigDecimal spent = transactionRepository.sumExpenseForCategoryAndPeriod(
                            user, budget.getCategory(), monthStart, monthEnd);
                    BigDecimal percentage = budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) > 0
                            ? spent.multiply(BigDecimal.valueOf(100)).divide(budget.getMonthlyLimit(), java.math.MathContext.DECIMAL64).setScale(2, MathContext.DECIMAL64.getRoundingMode())
                            : BigDecimal.ZERO;
                    return new BudgetStatus(budget, spent, percentage);
                })
                .toList();
    }

    public record BudgetStatus(Budget budget, BigDecimal spent, BigDecimal percentage) {
        public boolean isOverBudget() {
            return spent.compareTo(budget.getMonthlyLimit()) > 0;
        }
        public boolean isWarning() {
            return percentage.compareTo(BigDecimal.valueOf(80)) >= 0
                    && !isOverBudget();
        }
        public int progressBarWidth() {
            int pct = percentage.intValue();
            return Math.min(pct, 100);
        }
    }
}
