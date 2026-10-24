package com.studenthub.dto.note;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NoteRequest(
        @NotBlank(message = "Enter a title")
        @Size(max = 150, message = "Title must be 150 characters or fewer")
        String title,

        @NotBlank(message = "Write something in the note")
        @Size(max = 50000, message = "Notes can be up to 50,000 characters")
        String content,

        Long subjectId,

        @Size(max = 10, message = "Use up to 10 tags")
        Set<
                @NotBlank(message = "Tags cannot be empty")
                @Size(max = 30, message = "Each tag must be 30 characters or fewer")
                @Pattern(regexp = "^[\\p{L}\\p{N} _-]+$", message = "Tags can use letters, numbers, spaces, - and _")
                String> tags,

        Boolean pinned) {
}
