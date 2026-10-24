package com.studenthub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.studenthub.dto.attendance.AttendanceRequest;
import com.studenthub.dto.attendance.AttendanceResponse;
import com.studenthub.security.AppUserPrincipal;
import com.studenthub.service.AttendanceService;

import jakarta.validation.Valid;

/** Edit or delete a single attendance record (create/list live under /api/subjects/{id}/attendance). */
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PutMapping("/{id}")
    public AttendanceResponse update(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody AttendanceRequest request) {
        return attendanceService.update(principal.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
        attendanceService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
