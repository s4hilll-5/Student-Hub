package com.studenthub.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * A StudentHub account. Academic records reference the user through their own
 * user_id foreign keys; there are deliberately no collections here, so loading a
 * user never drags in their whole history.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /** Stored lower-cased; unique. */
    @Column(nullable = false, length = 254, unique = true)
    private String email;

    /** BCrypt hash. Never serialised: API responses use UserResponse instead of this entity. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.STUDENT;

    @Column(length = 150)
    private String college;

    @Column(length = 100)
    private String course;

    private Integer semester;

    /** IANA zone id such as "Asia/Kolkata"; decides what "today" means for this student. */
    @Column(nullable = false, length = 64)
    private String timezone = "UTC";

    @Column(name = "attendance_target", nullable = false, precision = 5, scale = 2)
    private BigDecimal attendanceTarget = new BigDecimal("75.00");

    @Column(name = "weekly_study_goal_hours", nullable = false)
    private Integer weeklyStudyGoalHours = 10;

    /**
     * Embedded in every access token. Incrementing it (password change, reset,
     * account disabled) invalidates all access tokens issued before.
     */
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 0;

    @Column(nullable = false)
    private boolean enabled = true;

    public void incrementTokenVersion() {
        this.tokenVersion = this.tokenVersion + 1;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public Integer getSemester() {
        return semester;
    }

    public void setSemester(Integer semester) {
        this.semester = semester;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public BigDecimal getAttendanceTarget() {
        return attendanceTarget;
    }

    public void setAttendanceTarget(BigDecimal attendanceTarget) {
        this.attendanceTarget = attendanceTarget;
    }

    public Integer getWeeklyStudyGoalHours() {
        return weeklyStudyGoalHours;
    }

    public void setWeeklyStudyGoalHours(Integer weeklyStudyGoalHours) {
        this.weeklyStudyGoalHours = weeklyStudyGoalHours;
    }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(Integer tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
