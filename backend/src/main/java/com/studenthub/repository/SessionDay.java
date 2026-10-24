package com.studenthub.repository;

import java.time.LocalDate;

/** JPQL constructor-expression projection used by analytics. */
public record SessionDay(LocalDate sessionDate, Integer actualMinutes, Boolean completed) {
}
