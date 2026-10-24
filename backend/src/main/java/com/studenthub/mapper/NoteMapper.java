package com.studenthub.mapper;

import java.util.List;

import com.studenthub.dto.note.NoteResponse;
import com.studenthub.entity.Note;

public final class NoteMapper {

    private NoteMapper() {
    }

    public static NoteResponse toResponse(Note note) {
        List<String> tags = note.getTags().stream().sorted().toList();
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                SubjectMapper.toSummary(note.getSubject()),
                tags,
                note.isPinned(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}
