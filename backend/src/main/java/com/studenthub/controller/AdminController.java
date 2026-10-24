package com.studenthub.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.admin.AdminStatsResponse;
import com.studenthub.dto.admin.AdminUserResponse;
import com.studenthub.dto.admin.UserStatusRequest;
import com.studenthub.dto.common.PageResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.AdminService;

import jakarta.validation.Valid;

/**
 * ADMIN only. Protected twice: by the URL rule in SecurityConfig and by @PreAuthorize here,
 * so a future routing change cannot accidentally expose it. Students get 403.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return adminService.listUsers(search, pageable);
    }

    @GetMapping("/stats")
    public AdminStatsResponse stats() {
        return adminService.stats();
    }

    @PatchMapping("/users/{id}/status")
    public AdminUserResponse setStatus(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request) {
        return adminService.setEnabled(principal.getId(), id, request.enabled());
    }
}
