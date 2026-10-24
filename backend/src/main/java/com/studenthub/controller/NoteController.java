package com.studenthub.controller;

import java.net.URI;
import java.util.List;

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

import com.studenthub.dto.common.PageResponse;
import com.studenthub.dto.note.NotePinRequest;
import com.studenthub.dto.note.NoteRequest;
import com.studenthub.dto.note.NoteResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.NoteService;

import jakarta.validation.Valid;

/** GET /api/notes?search=&subjectId=&tag=&pinned=&page=0&size=12&sort=updatedAt,desc (pinned notes first). */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public PageResponse<NoteResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean pinned,
            @PageableDefault(size = 12) Pageable pageable) {
        return noteService.list(principal.getId(), search, subjectId, tag, pinned, pageable);
    }

    /** Distinct tags used across the user's notes, for the filter. */
    @GetMapping("/tags")
    public List<String> tags(@AuthenticationPrincipal AppUserPrincipal principal) {
        return noteService.tags(principal.getId());
    }

    @GetMapping("/{id}")
    public NoteResponse get(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        return noteService.get(principal.getId(), id);
    }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@AuthenticationPrincipal AppUserPrincipal principal,
            @Valid @RequestBody NoteRequest request) {
        NoteResponse created = noteService.create(principal.getId(), request);
        return ResponseEntity.created(URI.create("/api/notes/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public NoteResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody NoteRequest request) {
        return noteService.update(principal.getId(), id, request);
    }

    @PatchMapping("/{id}/pin")
    public NoteResponse pin(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody NotePinRequest request) {
        return noteService.setPinned(principal.getId(), id, request.pinned());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        noteService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
