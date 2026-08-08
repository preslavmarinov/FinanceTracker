package com.finance.tracker.service;

import com.finance.tracker.dto.TransactionFilterDto;
import com.finance.tracker.model.Transaction;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.TransactionRepository;
import com.finance.tracker.spec.TransactionSpec;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<Transaction> findAll(User user, TransactionFilterDto filter) {
        Specification<Transaction> spec = Specification.where(TransactionSpec.ofUser(user))
                .and(TransactionSpec.ofType(filter.getType()))
                .and(TransactionSpec.ofCategory(filter.getCategoryId()))
                .and(TransactionSpec.dateFrom(filter.getDateFrom()))
                .and(TransactionSpec.dateTo(filter.getDateTo()))
                .and(TransactionSpec.amountMin(filter.getAmountMin()))
                .and(TransactionSpec.amountMax(filter.getAmountMax()))
                .and(TransactionSpec.titleContains(filter.getKeyword()));

        return transactionRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "date", "id"));
    }

    public Transaction findById(Long id, User user) {
        return transactionRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
    }

    @Transactional
    public Transaction save(Transaction transaction, User user) {
        transaction.setUser(user);
        return transactionRepository.save(transaction);
    }

    @Transactional
    public void delete(Long id, User user) {
        Transaction transaction = findById(id, user);
        transactionRepository.delete(transaction);
    }

    public DashboardStats getDashboardStats(User user) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal monthIncome = transactionRepository.sumIncomeForPeriod(user, monthStart, monthEnd);
        BigDecimal monthExpense = transactionRepository.sumExpenseForPeriod(user, monthStart, monthEnd);
        BigDecimal totalIncome = transactionRepository.sumAllIncome(user);
        BigDecimal totalExpense = transactionRepository.sumAllExpense(user);

        return new DashboardStats(monthIncome, monthExpense, totalIncome, totalExpense);
    }

    public List<PieSlice> getMonthlyExpensesByCategory(User user) {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        LocalDate monthEnd = now.withDayOfMonth(now.lengthOfMonth());

        List<Object[]> rows = transactionRepository.findExpensesByCategoryForPeriod(user, monthStart, monthEnd);
        List<PieSlice> slices = new ArrayList<>();
        for (Object[] row : rows) {
            slices.add(new PieSlice((String) row[0], ((BigDecimal) row[1]).doubleValue()));
        }
        return slices;
    }

    public record PieSlice(String label, double value) {}

    public record DashboardStats(
            BigDecimal monthIncome,
            BigDecimal monthExpense,
            BigDecimal totalIncome,
            BigDecimal totalExpense
    ) {
        public BigDecimal monthBalance() {
            return monthIncome.subtract(monthExpense);
        }

        public BigDecimal netBalance() {
            return totalIncome.subtract(totalExpense);
        }
    }
}
