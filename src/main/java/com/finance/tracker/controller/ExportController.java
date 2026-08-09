package com.finance.tracker.controller;

import com.finance.tracker.dto.ExportRequestDto;
import com.finance.tracker.model.User;
import com.finance.tracker.service.ExportService;
import com.finance.tracker.service.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/export")
public class ExportController {

    private final UserService userService;
    private final ExportService exportService;

    public ExportController(UserService userService, ExportService exportService) {
        this.userService = userService;
        this.exportService = exportService;
    }

    @GetMapping
    public String showExportPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();

        Map<Integer, String> monthNames = new LinkedHashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthNames.put(m, Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        }

        model.addAttribute("currentYear", currentYear);
        model.addAttribute("currentMonth", now.getMonthValue());
        model.addAttribute("monthNames", monthNames);
        model.addAttribute("years", List.of(currentYear, currentYear - 1, currentYear - 2,
                currentYear - 3, currentYear - 4));
        model.addAttribute("currentUser", user);
        return "export";
    }

    @PostMapping("/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal UserDetails userDetails,
                                           @ModelAttribute ExportRequestDto dto) {
        User user = userService.findByUsername(userDetails.getUsername());

        byte[] csv;
        String filename;

        if ("yearly".equals(dto.getReportType())) {
            csv = exportService.generateYearlyReport(user, dto.getYear());
            filename = "finance-report-" + dto.getYear() + ".csv";
        } else {
            csv = exportService.generateMonthlyReport(user, dto.getYear(), dto.getMonth());
            String monthName = Month.of(dto.getMonth())
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
            filename = "finance-report-" + monthName + "-" + dto.getYear() + ".csv";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
