package com.studenthub.dto.admin;

/** Platform-wide counts for administrators. Contains no student content. */
public record AdminStatsResponse(
        long totalUsers,
        long students,
        long admins,
        long disabledUsers,
        long assignments,
        long subjects,
        long notes,
        long examinations) {
}
