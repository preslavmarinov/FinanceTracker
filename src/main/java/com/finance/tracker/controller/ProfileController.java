package com.finance.tracker.controller;

import com.finance.tracker.dto.ChangePasswordDto;
import com.finance.tracker.dto.UpdateProfileDto;
import com.finance.tracker.model.User;
import com.finance.tracker.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());

        if (!model.containsAttribute("profileDto")) {
            UpdateProfileDto dto = new UpdateProfileDto();
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            dto.setUsername(user.getUsername());
            model.addAttribute("profileDto", dto);
        }
        if (!model.containsAttribute("passwordDto")) {
            model.addAttribute("passwordDto", new ChangePasswordDto());
        }

        model.addAttribute("memberSince", user.getCreatedAt());
        model.addAttribute("currentUser", user);
        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute("profileDto") UpdateProfileDto dto,
                                BindingResult result,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest request) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.profileDto", result);
            redirectAttributes.addFlashAttribute("profileDto", dto);
            redirectAttributes.addFlashAttribute("profileError", "Please correct the highlighted fields.");
            return "redirect:/profile";
        }

        User user = userService.findByUsername(userDetails.getUsername());
        String oldUsername = user.getUsername();

        try {
            User updated = userService.updateProfile(user, dto);
            if (!oldUsername.equals(updated.getUsername())) {
                UserDetails newPrincipal = userService.loadUserByUsername(updated.getUsername());
                UsernamePasswordAuthenticationToken newAuth =
                        UsernamePasswordAuthenticationToken.authenticated(
                                newPrincipal, null, newPrincipal.getAuthorities());
                SecurityContext newContext = SecurityContextHolder.createEmptyContext();
                newContext.setAuthentication(newAuth);
                SecurityContextHolder.setContext(newContext);
                request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, newContext);
            }
            redirectAttributes.addFlashAttribute("profileSuccess", "Profile updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("profileError", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @Valid @ModelAttribute("passwordDto") ChangePasswordDto dto,
                                 BindingResult result,
                                 RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("passwordError", "Please fill in all password fields correctly.");
            return "redirect:/profile";
        }

        User user = userService.findByUsername(userDetails.getUsername());
        try {
            userService.changePassword(user, dto);
            redirectAttributes.addFlashAttribute("passwordSuccess", "Password changed successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("passwordError", e.getMessage());
        }
        return "redirect:/profile";
    }
}
