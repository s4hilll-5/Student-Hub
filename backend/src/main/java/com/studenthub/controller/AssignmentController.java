package com.studenthub.controller;

import java.net.URI;
import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.assignment.AssignmentFilter;
import com.studenthub.dto.assignment.AssignmentRequest;
import com.studenthub.dto.assignment.AssignmentResponse;
import com.studenthub.dto.assignment.AssignmentStatusRequest;
import com.studenthub.dto.common.PageResponse;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Priority;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.AssignmentService;

import jakarta.validation.Valid;

/**
 * GET /api/assignments?search=&subjectId=&priority=HIGH&status=PENDING&overdue=true
 *     &dueFrom=2026-10-01T00:00:00Z&dueTo=...&page=0&size=10&sort=dueAt,asc
 */
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public PageResponse<AssignmentResponse> list(
            @AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) AssignmentStatus status,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Instant dueFrom,
            @RequestParam(required = false) Instant dueTo,
            @PageableDefault(size = 10) Pageable pageable) {
        AssignmentFilter filter = new AssignmentFilter(search, subjectId, priority, status, overdue, dueFrom, dueTo);
        return assignmentService.list(principal.getId(), filter, pageable);
    }

    @GetMapping("/{id}")
    public AssignmentResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return assignmentService.get(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse created = assignmentService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/assignments/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public AssignmentResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        return assignmentService.update(principal.getId(), id, request);
    }

    /** Quick status change, e.g. "mark completed" from the list. */
    @PatchMapping("/{id}/status")
    public AssignmentResponse updateStatus(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody AssignmentStatusRequest request) {
        return assignmentService.updateStatus(principal.getId(), id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        assignmentService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
