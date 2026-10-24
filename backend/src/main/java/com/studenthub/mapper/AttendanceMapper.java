package com.studenthub.mapper;

import com.studenthub.dto.attendance.AttendanceResponse;
import com.studenthub.entity.AttendanceRecord;

public final class AttendanceMapper {

    private AttendanceMapper() {
    }

    public static AttendanceResponse toResponse(AttendanceRecord record) {
        return new AttendanceResponse(
                record.getId(),
                record.getSubject().getId(),
                record.getClassDate(),
                record.getStatus(),
                record.getNotes(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
