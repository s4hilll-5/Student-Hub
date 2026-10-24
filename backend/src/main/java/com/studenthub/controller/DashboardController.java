package com.studenthub.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.dashboard.AnalyticsResponse;
import com.studenthub.dto.dashboard.DashboardSummary;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.AnalyticsService;
import com.studenthub.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final AnalyticsService analyticsService;

    public DashboardController(DashboardService dashboardService, AnalyticsService analyticsService) {
        this.dashboardService = dashboardService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@AuthenticationPrincipal AppUserPrincipal principal) {
        return dashboardService.summary(principal.getId());
    }

    /** ?weeks=8 (1-26): how many weeks of history the weekly charts cover. */
    @GetMapping("/analytics")
    public AnalyticsResponse analytics(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "" + AnalyticsService.DEFAULT_WEEKS) int weeks) {
        return analyticsService.analytics(principal.getId(), weeks);
    }
}
