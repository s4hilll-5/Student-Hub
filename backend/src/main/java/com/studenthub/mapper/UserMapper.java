package com.studenthub.mapper;

import com.studenthub.dto.admin.AdminUserResponse;
import com.studenthub.dto.user.UserResponse;
import com.studenthub.entity.User;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getCollege(),
                user.getCourse(),
                user.getSemester(),
                user.getTimezone(),
                user.getAttendanceTarget(),
                user.getWeeklyStudyGoalHours(),
                user.getCreatedAt());
    }

    public static AdminUserResponse toAdminResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCollege(),
                user.getCreatedAt());
    }
}
