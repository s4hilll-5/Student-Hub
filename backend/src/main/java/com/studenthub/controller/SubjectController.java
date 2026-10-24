package com.studenthub.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.attendance.AttendanceRequest;
import com.studenthub.dto.attendance.AttendanceResponse;
import com.studenthub.dto.common.PageResponse;
import com.studenthub.dto.subject.SubjectRequest;
import com.studenthub.dto.subject.SubjectResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.AttendanceService;
import com.studenthub.service.SubjectService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    private final SubjectService subjectService;
    private final AttendanceService attendanceService;

    public SubjectController(SubjectService subjectService, AttendanceService attendanceService) {
        this.subjectService = subjectService;
        this.attendanceService = attendanceService;
    }

    /** All subjects with calculated attendance. ?belowTarget=true returns only those under target. */
    @GetMapping
    public List<SubjectResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(defaultValue = "false") boolean belowTarget) {
        return subjectService.list(principal.getId(), belowTarget);
    }

    @GetMapping("/{id}")
    public SubjectResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return subjectService.get(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<SubjectResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody SubjectRequest request) {
        SubjectResponse created = subjectService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/subjects/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public SubjectResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody SubjectRequest request) {
        return subjectService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        subjectService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }

    /** Attendance history, newest first. Optional ?from=2026-08-01&to=2026-10-31 (inclusive). */
    @GetMapping("/{id}/attendance")
    public PageResponse<AttendanceResponse> attendance(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        return attendanceService.list(principal.getId(), id, from, to, pageable);
    }

    @PostMapping("/{id}/attendance")
    public ResponseEntity<AttendanceResponse> recordAttendance(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody AttendanceRequest request) {
        AttendanceResponse created = attendanceService.create(principal.getId(), id, request);
        return ResponseEntity.created(URI.create("/api/attendance/" + created.id())).body(created);
    }
}
