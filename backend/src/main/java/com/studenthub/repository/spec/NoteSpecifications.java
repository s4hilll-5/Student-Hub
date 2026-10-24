package com.studenthub.repository.spec;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import com.studenthub.entity.Note;

import jakarta.persistence.criteria.Join;

public final class NoteSpecifications {

    private NoteSpecifications() {
    }

    public static Specification<Note> forFilter(Long userId, String search, Long subjectId, String tag, Boolean pinned) {
        Specification<Note> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
        if (SpecUtils.hasText(search)) {
            String pattern = SpecUtils.containsPattern(search);
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern, SpecUtils.ESCAPE),
                    cb.like(cb.lower(root.get("content")), pattern, SpecUtils.ESCAPE)));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }
        if (SpecUtils.hasText(tag)) {
            String normalised = tag.trim().toLowerCase(Locale.ROOT);
            spec = spec.and((root, query, cb) -> {
                // A note has each tag at most once, so this join cannot duplicate rows.
                Join<Note, String> tags = root.join("tags");
                return cb.equal(tags, normalised);
            });
        }
        if (pinned != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pinned"), pinned));
        }
        return spec;
    }
}
