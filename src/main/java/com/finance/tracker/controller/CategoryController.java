package com.finance.tracker.controller;

import com.finance.tracker.model.Category;
import com.finance.tracker.model.TransactionType;
import com.finance.tracker.model.User;
import com.finance.tracker.service.CategoryService;
import com.finance.tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("categories", categoryService.findAll(user));
        model.addAttribute("currentUser", user);
        return "categories/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("category", new Category());
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("currentUser", user);
        return "categories/form";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute Category category,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (result.hasErrors()) {
            model.addAttribute("types", TransactionType.values());
            model.addAttribute("currentUser", user);
            return "categories/form";
        }
        try {
            categoryService.save(category, user);
            redirectAttributes.addFlashAttribute("success", "Category created.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("types", TransactionType.values());
            model.addAttribute("currentUser", user);
            return "categories/form";
        }
        return "redirect:/categories";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long id, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("category", categoryService.findById(id, user));
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("currentUser", user);
        return "categories/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         @Valid @ModelAttribute Category category,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (result.hasErrors()) {
            model.addAttribute("types", TransactionType.values());
            model.addAttribute("currentUser", user);
            return "categories/form";
        }
        category.setId(id);
        categoryService.save(category, user);
        redirectAttributes.addFlashAttribute("success", "Category updated.");
        return "redirect:/categories";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(userDetails.getUsername());
        try {
            categoryService.delete(id, user);
            redirectAttributes.addFlashAttribute("success", "Category deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete — category is in use.");
        }
        return "redirect:/categories";
    }
}
