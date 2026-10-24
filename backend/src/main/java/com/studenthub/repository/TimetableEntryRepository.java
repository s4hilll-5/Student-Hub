package com.studenthub.repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studenthub.entity.TimetableEntry;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    @EntityGraph(attributePaths = "subject")
    List<TimetableEntry> findByUserId(Long userId);

    @EntityGraph(attributePaths = "subject")
    List<TimetableEntry> findByUserIdAndDayOfWeekOrderByStartTimeAsc(Long userId, DayOfWeek dayOfWeek);

    @EntityGraph(attributePaths = "subject")
    Optional<TimetableEntry> findByIdAndUserId(Long id, Long userId);

    /**
     * Slots on the same day that overlap [start, end). Touching slots (one ends at 10:00,
     * the next starts at 10:00) do not overlap. Pass -1 as excludeId when creating.
     */
    @Query("""
            select t from TimetableEntry t join fetch t.subject
            where t.user.id = :userId
              and t.dayOfWeek = :day
              and t.startTime < :end
              and t.endTime > :start
              and t.id <> :excludeId
            order by t.startTime
            """)
    List<TimetableEntry> findOverlapping(
            @Param("userId") Long userId,
            @Param("day") DayOfWeek day,
            @Param("start") LocalTime start,
            @Param("end") LocalTime end,
            @Param("excludeId") Long excludeId);
}
