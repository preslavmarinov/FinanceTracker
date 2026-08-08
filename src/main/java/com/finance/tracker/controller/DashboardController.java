package com.finance.tracker.controller;

import com.finance.tracker.dto.TransactionFilterDto;
import com.finance.tracker.model.User;
import com.finance.tracker.service.TransactionService;
import com.finance.tracker.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Collections;

@Controller
@RequestMapping({"/dashboard", "/"})
public class DashboardController {
    private final TransactionService transactionService;
    private final UserService userService;

    public DashboardController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/auth/login";
        }

        User user = userService.findByUsername(userDetails.getUsername());
        TransactionService.DashboardStats stats = transactionService.getDashboardStats(user);

        var recentTransactions = transactionService.findAll(user, new TransactionFilterDto());
        if (recentTransactions == null) {
            recentTransactions = Collections.emptyList();
        }

        int previewCount = Math.min(recentTransactions.size(), 5);
        var previewList = recentTransactions.subList(0, previewCount);

        model.addAttribute("stats", stats);
        model.addAttribute("recentTransactions", previewList);
        model.addAttribute("pieChartData", transactionService.getMonthlyExpensesByCategory(user));
        model.addAttribute("currentUser", user);

        return "dashboard";
    }
}
