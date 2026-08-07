package com.finance.tracker.service;

import com.finance.tracker.model.Category;
import com.finance.tracker.model.TransactionType;
import com.finance.tracker.model.User;
import com.finance.tracker.repository.CategoryRepository;
import com.finance.tracker.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           TransactionRepository transactionRepository) {
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<Category> findAll(User user) {
        return categoryRepository.findByUserOrderByNameAsc(user);
    }

    public List<Category> findByType(User user, TransactionType type) {
        if (type == null) return findAll(user);
        return categoryRepository.findByUserAndTypeOrderByNameAsc(user, type);
    }

    public Category findById(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));
    }

    @Transactional
    public Category save(Category category, User user) {
        if (categoryRepository.existsByNameAndUser(category.getName(), user)
                && (category.getId() == null)) {
            throw new IllegalArgumentException("A category with that name already exists");
        }
        category.setUser(user);
        return categoryRepository.save(category);
    }

    @Transactional
    public void delete(Long id, User user) {
        Category category = findById(id, user);
        categoryRepository.delete(category);
    }
}
