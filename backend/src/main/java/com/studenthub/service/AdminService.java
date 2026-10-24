package com.studenthub.service;

import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.admin.AdminStatsResponse;
import com.studenthub.dto.admin.AdminUserResponse;
import com.studenthub.dto.common.PageResponse;
import com.studenthub.entity.RevokeReason;
import com.studenthub.entity.Role;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.mapper.UserMapper;
import com.studenthub.repository.AssignmentRepository;
import com.studenthub.repository.ExaminationRepository;
import com.studenthub.repository.NoteRepository;
import com.studenthub.repository.SubjectRepository;
import com.studenthub.repository.UserRepository;

/** ADMIN-only operations. Admins see account metadata and totals, never students' academic content. */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "fullName", "email");

    private final UserRepository userRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final NoteRepository noteRepository;
    private final ExaminationRepository examRepository;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;

    public AdminService(UserRepository userRepository, AssignmentRepository assignmentRepository,
            SubjectRepository subjectRepository, NoteRepository noteRepository, ExaminationRepository examRepository,
            RefreshTokenService refreshTokenService, UserService userService) {
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.noteRepository = noteRepository;
        this.examRepository = examRepository;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
    }

    public PageResponse<AdminUserResponse> listUsers(String search, Pageable pageable) {
        Pageable page = PageRequests.sanitize(pageable, SORTABLE, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> users = (search == null || search.isBlank())
                ? userRepository.findAll(page)
                : userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        search.trim(), search.trim(), page);
        return PageResponse.of(users, UserMapper::toAdminResponse);
    }

    public AdminStatsResponse stats() {
        return new AdminStatsResponse(
                userRepository.count(),
                userRepository.countByRole(Role.STUDENT),
                userRepository.countByRole(Role.ADMIN),
                userRepository.countByEnabledFalse(),
                assignmentRepository.count(),
                subjectRepository.count(),
                noteRepository.count(),
                examRepository.count());
    }

    /** Disabling signs the user out everywhere immediately. Admins cannot disable themselves. */
    @Transactional
    public AdminUserResponse setEnabled(Long adminId, Long userId, boolean enabled) {
        if (adminId.equals(userId) && !enabled) {
            throw new BadRequestException("You can't disable your own account.", "enabled");
        }
        User user = userService.requireUser(userId);
        if (user.isEnabled() != enabled) {
            user.setEnabled(enabled);
            if (!enabled) {
                user.incrementTokenVersion();
                refreshTokenService.revokeAll(userId, RevokeReason.ACCOUNT_DISABLED);
            }
        }
        return UserMapper.toAdminResponse(user);
    }
}
