package com.studenthub.repository.spec;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.studenthub.entity.Examination;
import com.studenthub.entity.ExamType;

public final class ExamSpecifications {

    private ExamSpecifications() {
    }

    /**
     * @param upcoming true = exams at or after now, false = past exams, null = all
     * @param from     inclusive lower bound on examAt (calendar month views)
     * @param to       exclusive upper bound on examAt
     */
    public static Specification<Examination> forFilter(
            Long userId, Boolean upcoming, Long subjectId, ExamType type, Instant from, Instant to, Instant now) {
        Specification<Examination> spec = (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
        if (upcoming != null) {
            spec = spec.and(upcoming
                    ? (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("examAt"), now)
                    : (root, query, cb) -> cb.lessThan(root.get("examAt"), now));
        }
        if (subjectId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("examType"), type));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("examAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("examAt"), to));
        }
        return spec;
    }
}
