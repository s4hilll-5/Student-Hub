package com.studenthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.studenthub.dto.assignment.AssignmentRequest;
import com.studenthub.dto.assignment.AssignmentResponse;
import com.studenthub.entity.Assignment;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.entity.Priority;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.repository.AssignmentRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.support.TestData;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-16T10:00:00Z");
    private static final long OWNER_ID = 7L;

    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectService subjectService;

    private AssignmentService service;

    @BeforeEach
    void setUp() {
        service = new AssignmentService(assignmentRepository, userRepository, subjectService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static AssignmentRequest request(Instant dueAt, AssignmentStatus status) {
        return new AssignmentRequest("  Lab report  ", "Experiment 4", null, dueAt, Priority.HIGH, status);
    }

    private static Assignment existing(long id, User owner, Instant dueAt, AssignmentStatus status) {
        Assignment assignment = new Assignment();
        assignment.setId(id);
        assignment.setUser(owner);
        assignment.setTitle("Essay");
        assignment.setDueAt(dueAt);
        assignment.setPriority(Priority.MEDIUM);
        assignment.setStatus(status);
        return assignment;
    }

    @Test
    void createUsesTheAuthenticatedUserAsOwnerAndDefaultsToPending() {
        User owner = TestData.user(OWNER_ID);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssignmentResponse response = service.create(OWNER_ID, request(NOW.plusSeconds(86_400), null));

        ArgumentCaptor<Assignment> saved = ArgumentCaptor.forClass(Assignment.class);
        verify(assignmentRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(owner);
        assertThat(saved.getValue().getTitle()).isEqualTo("Lab report");
        assertThat(response.status()).isEqualTo(AssignmentStatus.PENDING);
        assertThat(response.overdue()).isFalse();
        assertThat(response.completedAt()).isNull();
    }

    @Test
    void createRejectsASubjectThatBelongsToSomeoneElse() {
        AssignmentRequest withForeignSubject = new AssignmentRequest(
                "Lab report", null, 99L, NOW.plusSeconds(3600), Priority.LOW, null);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(TestData.user(OWNER_ID));
        when(subjectService.resolveOptional(OWNER_ID, 99L))
                .thenThrow(new BadRequestException("Choose one of your subjects.", "subjectId"));

        assertThatThrownBy(() -> service.create(OWNER_ID, withForeignSubject))
                .isInstanceOf(BadRequestException.class)
                .extracting("field").isEqualTo("subjectId");
        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void anotherUsersAssignmentIsReportedAsNotFound() {
        when(assignmentRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(OWNER_ID, 5L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(OWNER_ID, 5L)).isInstanceOf(ResourceNotFoundException.class);
        verify(assignmentRepository, never()).delete(any());
    }

    @Test
    void completingSetsCompletedAtAndReopeningClearsIt() {
        Assignment assignment = existing(5L, TestData.user(OWNER_ID), NOW.minusSeconds(60), AssignmentStatus.PENDING);
        when(assignmentRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(assignment));

        AssignmentResponse completed = service.updateStatus(OWNER_ID, 5L, AssignmentStatus.COMPLETED);
        assertThat(completed.completedAt()).isEqualTo(NOW);
        assertThat(completed.overdue()).as("completed work is never overdue").isFalse();

        AssignmentResponse reopened = service.updateStatus(OWNER_ID, 5L, AssignmentStatus.IN_PROGRESS);
        assertThat(reopened.completedAt()).isNull();
        assertThat(reopened.overdue()).as("past due and not completed").isTrue();
    }

    @Test
    void overdueOnlyWhenPastDueAndNotCompleted() {
        User owner = TestData.user(OWNER_ID);
        assertThat(existing(1L, owner, NOW.minusSeconds(1), AssignmentStatus.PENDING).isOverdue(NOW)).isTrue();
        assertThat(existing(2L, owner, NOW.minusSeconds(1), AssignmentStatus.IN_PROGRESS).isOverdue(NOW)).isTrue();
        assertThat(existing(3L, owner, NOW.minusSeconds(1), AssignmentStatus.COMPLETED).isOverdue(NOW)).isFalse();
        assertThat(existing(4L, owner, NOW.plusSeconds(1), AssignmentStatus.PENDING).isOverdue(NOW)).isFalse();
        assertThat(existing(5L, owner, NOW, AssignmentStatus.PENDING).isOverdue(NOW)).as("due exactly now").isFalse();
    }

    @Test
    void updateWithoutStatusKeepsTheCurrentStatus() {
        Assignment assignment = existing(5L, TestData.user(OWNER_ID), NOW.plusSeconds(60), AssignmentStatus.IN_PROGRESS);
        when(assignmentRepository.findByIdAndUserId(5L, OWNER_ID)).thenReturn(Optional.of(assignment));

        AssignmentResponse response = service.update(OWNER_ID, 5L, request(NOW.plusSeconds(7200), null));

        assertThat(response.status()).isEqualTo(AssignmentStatus.IN_PROGRESS);
        assertThat(response.dueAt()).isEqualTo(NOW.plusSeconds(7200));
        assertThat(response.priority()).isEqualTo(Priority.HIGH);
    }
}
