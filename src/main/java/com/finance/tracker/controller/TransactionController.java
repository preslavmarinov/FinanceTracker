package com.finance.tracker.controller;

import com.finance.tracker.dto.TransactionFilterDto;
import com.finance.tracker.model.Category;
import com.finance.tracker.model.Transaction;
import com.finance.tracker.model.TransactionType;
import com.finance.tracker.model.User;
import com.finance.tracker.service.CategoryService;
import com.finance.tracker.service.TransactionService;
import com.finance.tracker.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final CategoryService categoryService;
    private final UserService userService;

    public TransactionController(TransactionService transactionService,
                                 CategoryService categoryService,
                                 UserService userService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails,
                       @ModelAttribute TransactionFilterDto filter,
                       Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        List<Transaction> transactions = transactionService.findAll(user, filter);
        List<Category> categories = categoryService.findAll(user);

        model.addAttribute("transactions", transactions);
        model.addAttribute("categories", categories);
        model.addAttribute("filter", filter);
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("currentUser", user);
        return "transactions/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("transaction", new Transaction());
        model.addAttribute("categories", categoryService.findAll(user));
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("currentUser", user);
        return "transactions/form";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute Transaction transaction,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll(user));
            model.addAttribute("types", TransactionType.values());
            model.addAttribute("currentUser", user);
            return "transactions/form";
        }
        transactionService.save(transaction, user);
        redirectAttributes.addFlashAttribute("success", "Transaction added successfully.");
        return "redirect:/transactions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long id, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        Transaction transaction = transactionService.findById(id, user);
        model.addAttribute("transaction", transaction);
        model.addAttribute("categories", categoryService.findAll(user));
        model.addAttribute("types", TransactionType.values());
        model.addAttribute("currentUser", user);
        return "transactions/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         @Valid @ModelAttribute Transaction transaction,
                         BindingResult result,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        transactionService.findById(id, user); // ownership check
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll(user));
            model.addAttribute("types", TransactionType.values());
            model.addAttribute("currentUser", user);
            return "transactions/form";
        }
        transaction.setId(id);
        transactionService.save(transaction, user);
        redirectAttributes.addFlashAttribute("success", "Transaction updated.");
        return "redirect:/transactions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(userDetails.getUsername());
        transactionService.delete(id, user);
        redirectAttributes.addFlashAttribute("success", "Transaction deleted.");
        return "redirect:/transactions";
    }
}
