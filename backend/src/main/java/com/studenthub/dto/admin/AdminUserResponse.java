package com.studenthub.dto.admin;

import java.time.Instant;

import com.studenthub.entity.Role;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        String college,
        Instant createdAt) {
}
