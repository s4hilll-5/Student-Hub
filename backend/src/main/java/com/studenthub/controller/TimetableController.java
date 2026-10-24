package com.studenthub.controller;

import java.net.URI;
import java.time.DayOfWeek;
import java.util.List;

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

import com.studenthub.dto.timetable.TimetableRequest;
import com.studenthub.dto.timetable.TimetableResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.TimetableService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {

    private final TimetableService timetableService;

    public TimetableController(TimetableService timetableService) {
        this.timetableService = timetableService;
    }

    /** Whole week, or ?day=MONDAY for a single day. */
    @GetMapping
    public List<TimetableResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) DayOfWeek day) {
        return timetableService.list(principal.getId(), day);
    }

    /** 409 if the slot overlaps another class on the same day. */
    @PostMapping
    public ResponseEntity<TimetableResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody TimetableRequest request) {
        TimetableResponse created = timetableService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/timetable/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public TimetableResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody TimetableRequest request) {
        return timetableService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        timetableService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
