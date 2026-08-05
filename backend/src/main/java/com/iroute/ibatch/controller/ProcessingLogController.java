package com.iroute.ibatch.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.iroute.ibatch.application.usecase.DashboardService;
import com.iroute.ibatch.dto.response.ProcessingLogResponse;

@RestController
@RequestMapping("/logs")
public class ProcessingLogController {

    private final DashboardService dashboardService;

    public ProcessingLogController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public com.iroute.ibatch.dto.response.PageResponse<ProcessingLogResponse> getLogs(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int size) {
        return dashboardService.findRecentLogs(page, size);
    }
}
