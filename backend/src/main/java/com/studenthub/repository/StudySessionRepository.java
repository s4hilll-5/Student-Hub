package com.studenthub.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studenthub.entity.StudySession;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

    @EntityGraph(attributePaths = "subject")
    List<StudySession> findByUserIdAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
            Long userId, LocalDate from, LocalDate to);

    @EntityGraph(attributePaths = "subject")
    List<StudySession> findByUserIdAndSessionDateOrderByStartTimeAsc(Long userId, LocalDate date);

    @EntityGraph(attributePaths = "subject")
    Optional<StudySession> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select s from StudySession s
            where s.user.id = :userId
              and s.sessionDate = :date
              and s.startTime < :end
              and s.endTime > :start
              and s.id <> :excludeId
            order by s.startTime
            """)
    List<StudySession> findOverlapping(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("excludeId") Long excludeId);

    @Query("""
            select coalesce(sum(s.actualMinutes), 0) from StudySession s
            where s.user.id = :userId and s.completed = true
              and s.sessionDate between :from and :to
            """)
    Long sumCompletedMinutes(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByUserIdAndSessionDateBetween(Long userId, LocalDate from, LocalDate to);

    long countByUserIdAndCompletedTrueAndSessionDateBetween(Long userId, LocalDate from, LocalDate to);

    /** Lightweight rows for weekly analytics. */
    @Query("""
            select new com.studenthub.repository.SessionDay(s.sessionDate, s.actualMinutes, s.completed)
            from StudySession s
            where s.user.id = :userId and s.sessionDate between :from and :to
            """)
    List<SessionDay> findSessionDays(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
