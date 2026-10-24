package com.studenthub.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.common.PageResponse;
import com.studenthub.dto.exam.ExamRequest;
import com.studenthub.dto.exam.ExamResponse;
import com.studenthub.entity.Examination;
import com.studenthub.entity.ExamType;
import com.studenthub.entity.PreparationStatus;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.ExamMapper;
import com.studenthub.repository.ExaminationRepository;
import com.studenthub.repository.UserRepository;
import com.studenthub.repository.spec.ExamSpecifications;

@Service
@Transactional(readOnly = true)
public class ExamService {

    static final Set<String> SORTABLE = Set.of("examAt", "title", "createdAt", "preparationStatus");

    private final ExaminationRepository examRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final Clock clock;

    public ExamService(ExaminationRepository examRepository, UserRepository userRepository,
            SubjectService subjectService, Clock clock) {
        this.examRepository = examRepository;
        this.userRepository = userRepository;
        this.subjectService = subjectService;
        this.clock = clock;
    }

    public PageResponse<ExamResponse> list(Long userId, Boolean upcoming, Long subjectId, ExamType type,
            Instant from, Instant to, Pageable pageable) {
        if (from != null && to != null && !to.isAfter(from)) {
            throw new BadRequestException("'to' must be after 'from'", "to");
        }
        Instant now = clock.instant();
        Pageable page = PageRequests.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.ASC, "examAt"));
        Page<Examination> result = examRepository.findAll(
                ExamSpecifications.forFilter(userId, upcoming, subjectId, type, from, to, now), page);
        return PageResponse.of(result, exam -> ExamMapper.toResponse(exam, now));
    }

    public ExamResponse get(Long userId, Long id) {
        return ExamMapper.toResponse(requireOwned(userId, id), clock.instant());
    }

    @Transactional
    public ExamResponse create(Long userId, ExamRequest request) {
        Examination exam = new Examination();
        exam.setUser(userRepository.getReferenceById(userId));
        apply(userId, exam, request);
        examRepository.save(exam);
        return ExamMapper.toResponse(exam, clock.instant());
    }

    @Transactional
    public ExamResponse update(Long userId, Long id, ExamRequest request) {
        Examination exam = requireOwned(userId, id);
        apply(userId, exam, request);
        return ExamMapper.toResponse(exam, clock.instant());
    }

    @Transactional
    public void delete(Long userId, Long id) {
        examRepository.delete(requireOwned(userId, id));
    }

    private Examination requireOwned(Long userId, Long id) {
        return examRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Examination", id));
    }

    private void apply(Long userId, Examination exam, ExamRequest request) {
        exam.setTitle(request.title().trim());
        exam.setSubject(subjectService.resolveOptional(userId, request.subjectId()));
        exam.setExamAt(request.examAt());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setExamType(request.examType());
        exam.setPreparationStatus(request.preparationStatus() != null
                ? request.preparationStatus()
                : (exam.getPreparationStatus() != null ? exam.getPreparationStatus() : PreparationStatus.NOT_STARTED));
        exam.setLocation(UserService.blankToNull(request.location()));
        exam.setSyllabusNotes(UserService.blankToNull(request.syllabusNotes()));
    }
}
