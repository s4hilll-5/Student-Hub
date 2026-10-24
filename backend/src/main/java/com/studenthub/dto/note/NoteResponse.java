package com.studenthub.dto.note;

import java.time.Instant;
import java.util.List;

import com.studenthub.dto.common.SubjectSummary;

public record NoteResponse(
        Long id,
        String title,
        String content,
        SubjectSummary subject,
        List<String> tags,
        boolean pinned,
        Instant createdAt,
        Instant updatedAt) {
}
