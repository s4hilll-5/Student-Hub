package com.studenthub.repository.spec;

import java.time.Instant;

import org.springframework.data.jpa.domain.Specification;

import com.studenthub.dto.assignment.AssignmentFilter;
import com.studenthub.entity.Assignment;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Priority;

/** Composable WHERE clauses for GET /api/assignments. Ownership is always applied first. */
public final class AssignmentSpecifications {

    private AssignmentSpecifications() {
    }

    public static Specification<Assignment> forFilter(Long userId, AssignmentFilter filter, Instant now) {
        Specification<Assignment> spec = ownedBy(userId);
        if (filter == null) {
            return spec;
        }
        if (SpecUtils.hasText(filter.search())) {
            spec = spec.and(textContains(filter.search()));
        }
        if (filter.subjectId() != null) {
            spec = spec.and(subjectIs(filter.subjectId()));
        }
        if (filter.priority() != null) {
            spec = spec.and(priorityIs(filter.priority()));
        }
        if (filter.status() != null) {
            spec = spec.and(statusIs(filter.status()));
        }
        if (filter.overdue() != null) {
            spec = spec.and(filter.overdue() ? overdue(now) : notOverdue(now));
        }
        if (filter.dueFrom() != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueAt"), filter.dueFrom()));
        }
        if (filter.dueTo() != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("dueAt"), filter.dueTo()));
        }
        return spec;
    }

    public static Specification<Assignment> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Assignment> textContains(String search) {
        String pattern = SpecUtils.containsPattern(search);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, SpecUtils.ESCAPE),
                cb.like(cb.lower(root.get("description")), pattern, SpecUtils.ESCAPE));
    }

    public static Specification<Assignment> subjectIs(Long subjectId) {
        return (root, query, cb) -> cb.equal(root.get("subject").get("id"), subjectId);
    }

    public static Specification<Assignment> priorityIs(Priority priority) {
        return (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Assignment> statusIs(AssignmentStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Assignment> overdue(Instant now) {
        return (root, query, cb) -> cb.and(
                cb.lessThan(root.get("dueAt"), now),
                cb.notEqual(root.get("status"), AssignmentStatus.COMPLETED));
    }

    public static Specification<Assignment> notOverdue(Instant now) {
        return (root, query, cb) -> cb.or(
                cb.greaterThanOrEqualTo(root.get("dueAt"), now),
                cb.equal(root.get("status"), AssignmentStatus.COMPLETED));
    }
}
