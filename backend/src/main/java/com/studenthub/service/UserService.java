package com.studenthub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.studenthub.dto.user.ChangePasswordRequest;
import com.studenthub.dto.user.DeleteAccountRequest;
import com.studenthub.dto.user.UpdateProfileRequest;
import com.studenthub.dto.user.UserResponse;
import com.studenthub.entity.RevokeReason;
import com.studenthub.entity.User;
import com.studenthub.exception.BadRequestException;
import com.studenthub.exception.ResourceNotFoundException;
import com.studenthub.mapper.UserMapper;
import com.studenthub.repository.UserRepository;
import com.studenthub.service.SessionTokenService.IssuedSession;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final SessionTokenService sessionTokenService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService, SessionTokenService sessionTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
        this.sessionTokenService = sessionTokenService;
    }

    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    public UserResponse getProfile(Long userId) {
        return UserMapper.toResponse(requireUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = requireUser(userId);
        user.setFullName(request.fullName().trim());
        user.setCollege(blankToNull(request.college()));
        user.setCourse(blankToNull(request.course()));
        user.setSemester(request.semester());
        user.setTimezone(request.timezone());
        user.setAttendanceTarget(request.attendanceTarget());
        user.setWeeklyStudyGoalHours(request.weeklyStudyGoalHours());
        return UserMapper.toResponse(user);
    }

    /**
     * Changes the password, signs out every other device, and returns a fresh session
     * for the current one so the user is not logged out of the tab they are using.
     */
    @Transactional
    public IssuedSession changePassword(Long userId, ChangePasswordRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.", "currentPassword");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Choose a password you haven't used here before.", "newPassword");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.incrementTokenVersion();
        refreshTokenService.revokeAll(userId, RevokeReason.PASSWORD_CHANGED);
        return sessionTokenService.issue(user);
    }

    /** Deletes the account; every academic record is removed by ON DELETE CASCADE. */
    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadRequestException("Password is incorrect.", "password");
        }
        userRepository.delete(user);
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
