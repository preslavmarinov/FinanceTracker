package com.finance.tracker.repository;

import com.finance.tracker.model.Transaction;
import com.finance.tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.finance.tracker.model.Category;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndUser(Long id, User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = 'INCOME' AND t.date BETWEEN :from AND :to")
    BigDecimal sumIncomeForPeriod(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = 'EXPENSE' AND t.date BETWEEN :from AND :to")
    BigDecimal sumExpenseForPeriod(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = 'INCOME'")
    BigDecimal sumAllIncome(@Param("user") User user);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = 'EXPENSE'")
    BigDecimal sumAllExpense(@Param("user") User user);

    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t WHERE t.user = :user AND t.type = 'EXPENSE' AND t.date BETWEEN :from AND :to AND t.category IS NOT NULL GROUP BY t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> findExpensesByCategoryForPeriod(@Param("user") User user, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.user = :user AND t.type = 'EXPENSE' AND t.category = :category AND t.date BETWEEN :from AND :to")
    BigDecimal sumExpenseForCategoryAndPeriod(@Param("user") User user, @Param("category") Category category, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
