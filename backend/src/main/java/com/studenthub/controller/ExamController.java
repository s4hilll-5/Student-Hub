package com.studenthub.controller;

import java.net.URI;
import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

import com.studenthub.dto.common.PageResponse;
import com.studenthub.dto.exam.ExamRequest;
import com.studenthub.dto.exam.ExamResponse;
import com.studenthub.entity.ExamType;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.ExamService;

import jakarta.validation.Valid;

/**
 * GET /api/exams?upcoming=true&subjectId=&type=MIDTERM&from=...&to=...&page=0&size=20&sort=examAt,asc
 * Default sort is nearest exam first.
 */
@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public PageResponse<ExamResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) Boolean upcoming,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) ExamType type,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20) Pageable pageable) {
        return examService.list(principal.getId(), upcoming, subjectId, type, from, to, pageable);
    }

    @GetMapping("/{id}")
    public ExamResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return examService.get(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<ExamResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody ExamRequest request) {
        ExamResponse created = examService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/exams/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ExamResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody ExamRequest request) {
        return examService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        examService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
