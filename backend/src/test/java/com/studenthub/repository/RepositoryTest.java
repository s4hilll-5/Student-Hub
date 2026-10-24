package com.studenthub.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.studenthub.dto.assignment.AssignmentFilter;
import com.studenthub.entity.Assignment;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.AttendanceRecord;
import com.studenthub.entity.AttendanceStatus;
import com.studenthub.entity.Priority;
import com.studenthub.entity.Subject;
import com.studenthub.entity.TimetableEntry;
import com.studenthub.entity.User;
import com.studenthub.repository.spec.AssignmentSpecifications;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRepository;
    @Autowired
    private TimetableEntryRepository timetableRepository;
    @Autowired
    private SubjectRepository subjectRepository;

    private final Instant now = Instant.now();
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = em.persist(user("alice"));
        bob = em.persist(user("bob"));
    }

    private static User user(String name) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(name + "-" + System.nanoTime() + "@example.com");
        user.setPasswordHash("hash");
        return user;
    }

    private Subject subject(User owner, String name) {
        Subject subject = new Subject();
        subject.setUser(owner);
        subject.setName(name);
        return em.persist(subject);
    }

    private Assignment assignment(User owner, Subject subject, String title, Instant dueAt, AssignmentStatus status) {
        Assignment assignment = new Assignment();
        assignment.setUser(owner);
        assignment.setSubject(subject);
        assignment.setTitle(title);
        assignment.setDueAt(dueAt);
        assignment.setPriority(Priority.MEDIUM);
        assignment.setStatus(status);
        return em.persist(assignment);
    }

    @Test
    void overdueCountIgnoresCompletedAndFutureWork() {
        assignment(alice, null, "late", now.minus(1, ChronoUnit.DAYS), AssignmentStatus.PENDING);
        assignment(alice, null, "late but done", now.minus(1, ChronoUnit.DAYS), AssignmentStatus.COMPLETED);
        assignment(alice, null, "future", now.plus(1, ChronoUnit.DAYS), AssignmentStatus.PENDING);
        assignment(bob, null, "bob's late", now.minus(1, ChronoUnit.DAYS), AssignmentStatus.PENDING);

        assertThat(assignmentRepository.countByUserIdAndStatusNotAndDueAtBefore(alice.getId(), AssignmentStatus.COMPLETED, now))
                .isEqualTo(1);
    }

    @Test
    void searchIsOwnerScopedAndTreatsWildcardsLiterally() {
        assignment(alice, null, "Score 50% of marks", now, AssignmentStatus.PENDING);
        assignment(alice, null, "Score 500 points", now, AssignmentStatus.PENDING);
        assignment(bob, null, "Score 50% too", now, AssignmentStatus.PENDING);

        var filter = new AssignmentFilter("50%", null, null, null, null, null, null);
        var page = assignmentRepository.findAll(
                AssignmentSpecifications.forFilter(alice.getId(), filter, now), PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(Assignment::getTitle).containsExactly("Score 50% of marks");
    }

    @Test
    void attendanceCountsAreGroupedPerSubjectAndStatus() {
        Subject maths = subject(alice, "Maths");
        for (AttendanceStatus status : List.of(AttendanceStatus.PRESENT, AttendanceStatus.PRESENT,
                AttendanceStatus.ABSENT, AttendanceStatus.CANCELLED)) {
            AttendanceRecord record = new AttendanceRecord();
            record.setSubject(maths);
            record.setClassDate(LocalDate.now().minusDays(1));
            record.setStatus(status);
            em.persist(record);
        }
        em.flush();

        var rows = attendanceRepository.countByStatusForUser(alice.getId());
        assertThat(rows).hasSize(3);
        assertThat(rows).filteredOn(r -> r.getStatus() == AttendanceStatus.PRESENT)
                .singleElement().extracting(AttendanceRecordRepository.SubjectStatusCount::getTotal).isEqualTo(2L);
        assertThat(attendanceRepository.countByStatusForUser(bob.getId())).isEmpty();
    }

    @Test
    void overlappingSlotsAreFoundButTouchingOnesAreNot() {
        Subject maths = subject(alice, "Maths");
        TimetableEntry entry = new TimetableEntry();
        entry.setUser(alice);
        entry.setSubject(maths);
        entry.setDayOfWeek(DayOfWeek.TUESDAY);
        entry.setStartTime(LocalTime.of(9, 0));
        entry.setEndTime(LocalTime.of(10, 0));
        em.persist(entry);
        em.flush();

        assertThat(timetableRepository.findOverlapping(alice.getId(), DayOfWeek.TUESDAY,
                LocalTime.of(9, 30), LocalTime.of(10, 30), -1L)).hasSize(1);
        assertThat(timetableRepository.findOverlapping(alice.getId(), DayOfWeek.TUESDAY,
                LocalTime.of(10, 0), LocalTime.of(11, 0), -1L)).isEmpty();
        assertThat(timetableRepository.findOverlapping(bob.getId(), DayOfWeek.TUESDAY,
                LocalTime.of(9, 30), LocalTime.of(10, 30), -1L)).isEmpty();
    }

    @Test
    void deletingASubjectRemovesItsAttendanceButKeepsAssignments() {
        Subject maths = subject(alice, "Maths");
        AttendanceRecord record = new AttendanceRecord();
        record.setSubject(maths);
        record.setClassDate(LocalDate.now());
        record.setStatus(AttendanceStatus.PRESENT);
        em.persist(record);
        Assignment homework = assignment(alice, maths, "Homework", now, AssignmentStatus.PENDING);
        em.flush();
        em.clear();

        subjectRepository.deleteById(maths.getId());
        em.flush();
        em.clear();

        assertThat(attendanceRepository.findById(record.getId())).isEmpty();
        Assignment reloaded = assignmentRepository.findById(homework.getId()).orElseThrow();
        assertThat(reloaded.getSubject()).isNull();
    }
}
