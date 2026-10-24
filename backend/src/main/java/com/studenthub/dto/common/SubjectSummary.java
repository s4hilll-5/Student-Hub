package com.studenthub.dto.common;

/** Compact subject reference embedded in assignments, exams, notes, classes and study sessions. */
public record SubjectSummary(Long id, String name, String code, String color) {
}
