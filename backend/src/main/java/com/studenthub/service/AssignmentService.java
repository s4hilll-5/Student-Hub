package com.studenthub.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.assignment.AssignmentFilter;
import com.studenthub.dto.assignment.AssignmentRequest;
import com.studenthub.dto.assignment.AssignmentResponse;
import com.studenthub.dto.common.PageResponse;
import com.studenthub.entity.Assignment;
import com.studenthub.entity.AssignmentStatus;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.AssignmentMapper;
import com.studenthub.repository.AssignmentRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.repository.spec.AssignmentSpecifications;

/**
 * Assignment CRUD. Every method takes the authenticated user's id (from the security
 * context, never from the request) and only ever reads or writes that user's rows.
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    static final Set<String> SORTABLE = Set.of("dueAt", "createdAt", "updatedAt", "title", "priority", "status");

    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final Clock clock;

    public AssignmentService(AssignmentRepository assignmentRepository, UserRepository userRepository,
            SubjectService subjectService, Clock clock) {
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.subjectService = subjectService;
        this.clock = clock;
    }

    public PageResponse<AssignmentResponse> list(Long userId, AssignmentFilter filter, Pageable pageable) {
        Instant now = clock.instant();
        Pageable page = PageRequests.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.ASC, "dueAt"));
        Page<Assignment> result = assignmentRepository.findAll(
                AssignmentSpecifications.forFilter(userId, filter, now), page);
        return PageResponse.of(result, assignment -> AssignmentMapper.toResponse(assignment, now));
    }

    public AssignmentResponse get(Long userId, Long id) {
        return AssignmentMapper.toResponse(requireOwned(userId, id), clock.instant());
    }

    @Transactional
    public AssignmentResponse create(Long userId, AssignmentRequest request) {
        Assignment assignment = new Assignment();
        assignment.setUser(userRepository.getReferenceById(userId));
        apply(userId, assignment, request);
        applyStatus(assignment, request.status() != null ? request.status() : AssignmentStatus.PENDING);
        assignmentRepository.save(assignment);
        return AssignmentMapper.toResponse(assignment, clock.instant());
    }

    @Transactional
    public AssignmentResponse update(Long userId, Long id, AssignmentRequest request) {
        Assignment assignment = requireOwned(userId, id);
        apply(userId, assignment, request);
        if (request.status() != null) {
            applyStatus(assignment, request.status());
        }
        return AssignmentMapper.toResponse(assignment, clock.instant());
    }

    @Transactional
    public AssignmentResponse updateStatus(Long userId, Long id, AssignmentStatus status) {
        Assignment assignment = requireOwned(userId, id);
        applyStatus(assignment, status);
        return AssignmentMapper.toResponse(assignment, clock.instant());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        assignmentRepository.delete(requireOwned(userId, id));
    }

    private Assignment requireOwned(Long userId, Long id) {
        return assignmentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
    }

    private void apply(Long userId, Assignment assignment, AssignmentRequest request) {
        assignment.setTitle(request.title().trim());
        assignment.setDescription(UserService.blankToNull(request.description()));
        assignment.setSubject(subjectService.resolveOptional(userId, request.subjectId()));
        assignment.setDueAt(request.dueAt());
        assignment.setPriority(request.priority());
    }

    /** Keeps completedAt in sync with the status (it feeds the completion-trend chart). */
    private void applyStatus(Assignment assignment, AssignmentStatus status) {
        if (status == AssignmentStatus.COMPLETED) {
            if (assignment.getStatus() != AssignmentStatus.COMPLETED || assignment.getCompletedAt() == null) {
                assignment.setCompletedAt(clock.instant());
            }
        } else {
            assignment.setCompletedAt(null);
        }
        assignment.setStatus(status);
    }
}
