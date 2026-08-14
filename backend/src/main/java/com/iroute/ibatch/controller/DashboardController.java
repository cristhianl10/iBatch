package com.iroute.ibatch.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.application.usecase.DashboardService;
import com.iroute.ibatch.dto.response.DashboardSummaryResponse;
import com.iroute.ibatch.dto.response.PageResponse;
import com.iroute.ibatch.dto.response.ProcessingLogResponse;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary() {
        return dashboardService.findSummary();
    }

    @GetMapping("/logs")
    public PageResponse<ProcessingLogResponse> findRecentLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return dashboardService.findRecentLogs(page, size);
    }
}
