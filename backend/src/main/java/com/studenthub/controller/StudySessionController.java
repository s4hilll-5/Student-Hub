package com.studenthub.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.studenthub.dto.study.StudySessionCompletionRequest;
import com.studenthub.dto.study.StudySessionRequest;
import com.studenthub.dto.study.StudySessionResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.StudySessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/study-sessions")
public class StudySessionController {

    private final StudySessionService studySessionService;

    public StudySessionController(StudySessionService studySessionService) {
        this.studySessionService = studySessionService;
    }

    /** ?from=2026-10-12&to=2026-10-18 (inclusive). Defaults to the current week. */
    @GetMapping
    public List<StudySessionResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return studySessionService.list(principal.getId(), from, to);
    }

    /** 409 if it overlaps another study session or a class. */
    @PostMapping
    public ResponseEntity<StudySessionResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody StudySessionRequest request) {
        StudySessionResponse created = studySessionService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/study-sessions/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public StudySessionResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody StudySessionRequest request) {
        return studySessionService.update(principal.getId(), id, request);
    }

    /** { "completed": true, "actualMinutes": 50 } or { "completed": false } to reopen. */
    @PatchMapping("/{id}/completion")
    public StudySessionResponse setCompletion(@AuthenticationPrincipal AppUserPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody StudySessionCompletionRequest request) {
        return studySessionService.setCompletion(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        studySessionService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
