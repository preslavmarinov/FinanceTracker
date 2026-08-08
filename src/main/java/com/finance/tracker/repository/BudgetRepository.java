package com.finance.tracker.repository;

import com.finance.tracker.model.Budget;
import com.finance.tracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserOrderByCategoryNameAsc(User user);
    Optional<Budget> findByIdAndUser(Long id, User user);
    boolean existsByCategoryIdAndUser(Long categoryId, User user);
}
