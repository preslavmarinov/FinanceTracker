package com.finance.tracker.controller;

import com.finance.tracker.model.Budget;
import com.finance.tracker.model.User;
import com.finance.tracker.service.BudgetService;
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
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final UserService userService;

    public BudgetController(BudgetService budgetService,
                            CategoryService categoryService,
                            UserService userService) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("budgetStatuses", budgetService.getBudgetStatuses(user));
        model.addAttribute("currentUser", user);
        return "budgets/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("budget", new Budget());
        model.addAttribute("categories", categoryService.findAll(user));
        model.addAttribute("currentUser", user);
        return "budgets/form";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute Budget budget,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll(user));
            model.addAttribute("currentUser", user);
            return "budgets/form";
        }
        try {
            budgetService.save(budget, user);
            redirectAttributes.addFlashAttribute("success", "Budget created.");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("categories", categoryService.findAll(user));
            model.addAttribute("currentUser", user);
            return "budgets/form";
        }
        return "redirect:/budgets";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long id, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("budget", budgetService.findById(id, user));
        model.addAttribute("categories", categoryService.findAll(user));
        model.addAttribute("currentUser", user);
        return "budgets/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         @Valid @ModelAttribute Budget budget,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll(user));
            model.addAttribute("currentUser", user);
            return "budgets/form";
        }
        budget.setId(id);
        budgetService.save(budget, user);
        redirectAttributes.addFlashAttribute("success", "Budget updated.");
        return "redirect:/budgets";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(userDetails.getUsername());
        budgetService.delete(id, user);
        redirectAttributes.addFlashAttribute("success", "Budget deleted.");
        return "redirect:/budgets";
    }
}
