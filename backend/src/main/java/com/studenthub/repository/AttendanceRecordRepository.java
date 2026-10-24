package com.studenthub.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.studenthub.entity.AttendanceRecord;
import com.studenthub.entity.AttendanceStatus;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Page<AttendanceRecord> findBySubjectId(Long subjectId, Pageable pageable);

    Page<AttendanceRecord> findBySubjectIdAndClassDateBetween(
            Long subjectId, LocalDate from, LocalDate to, Pageable pageable);

    /** Ownership goes through the subject: record -> subject -> user. */
    Optional<AttendanceRecord> findByIdAndSubjectUserId(Long id, Long userId);

    /** Row per (subject, status) with its count, for all of a user's subjects in one query. */
    @Query("""
            select r.subject.id as subjectId, r.status as status, count(r) as total
            from AttendanceRecord r
            where r.subject.user.id = :userId
            group by r.subject.id, r.status
            """)
    List<SubjectStatusCount> countByStatusForUser(@Param("userId") Long userId);

    @Query("""
            select r.subject.id as subjectId, r.status as status, count(r) as total
            from AttendanceRecord r
            where r.subject.id = :subjectId
            group by r.subject.id, r.status
            """)
    List<SubjectStatusCount> countByStatusForSubject(@Param("subjectId") Long subjectId);

    interface SubjectStatusCount {
        Long getSubjectId();

        AttendanceStatus getStatus();

        Long getTotal();
    }
}
