-- =====================================================================
-- StudentHub schema, version 1 (MySQL 8.0.16+ for enforced CHECK constraints)
--
-- Ownership model:
--   * Every academic table has user_id -> users(id) ON DELETE CASCADE,
--     so deleting an account removes all of that student's data.
--   * attendance_records and timetable_entries belong to a subject and are
--     deleted with it (ON DELETE CASCADE): they are meaningless without it.
--   * assignments, examinations, notes and study_sessions only *reference* a
--     subject (nullable, ON DELETE SET NULL): deleting a subject keeps the work.
--   * Instants are stored in DATETIME(6) as UTC. Dates/times that belong to the
--     student's calendar (class dates, timetable slots) use DATE / TIME.
-- =====================================================================

CREATE TABLE users (
    id                       BIGINT        NOT NULL AUTO_INCREMENT,
    full_name                VARCHAR(100)  NOT NULL,
    email                    VARCHAR(254)  NOT NULL,
    password_hash            VARCHAR(100)  NOT NULL,
    role                     ENUM('STUDENT','ADMIN') NOT NULL DEFAULT 'STUDENT',
    college                  VARCHAR(150)  NULL,
    course                   VARCHAR(100)  NULL,
    semester                 INT           NULL,
    timezone                 VARCHAR(64)   NOT NULL DEFAULT 'UTC',
    attendance_target        DECIMAL(5,2)  NOT NULL DEFAULT 75.00,
    weekly_study_goal_hours  INT           NOT NULL DEFAULT 10,
    token_version            INT           NOT NULL DEFAULT 0,
    enabled                  BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at               DATETIME(6)   NOT NULL,
    updated_at               DATETIME(6)   NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_attendance_target CHECK (attendance_target BETWEEN 1 AND 100),
    CONSTRAINT chk_users_semester CHECK (semester IS NULL OR semester BETWEEN 1 AND 12),
    CONSTRAINT chk_users_study_goal CHECK (weekly_study_goal_hours BETWEEN 1 AND 112)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE subjects (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    user_id            BIGINT        NOT NULL,
    name               VARCHAR(100)  NOT NULL,
    code               VARCHAR(20)   NULL,
    color              VARCHAR(7)    NOT NULL DEFAULT '#6366F1',
    target_percentage  DECIMAL(5,2)  NOT NULL DEFAULT 75.00,
    prior_attended     INT           NOT NULL DEFAULT 0,
    prior_total        INT           NOT NULL DEFAULT 0,
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    CONSTRAINT pk_subjects PRIMARY KEY (id),
    CONSTRAINT fk_subjects_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_subjects_user_name UNIQUE (user_id, name),
    CONSTRAINT uk_subjects_user_code UNIQUE (user_id, code),
    CONSTRAINT chk_subjects_target CHECK (target_percentage BETWEEN 1 AND 100),
    CONSTRAINT chk_subjects_prior CHECK (prior_attended >= 0 AND prior_total >= 0 AND prior_attended <= prior_total)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE attendance_records (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    subject_id  BIGINT        NOT NULL,
    class_date  DATE          NOT NULL,
    status      ENUM('PRESENT','ABSENT','CANCELLED') NOT NULL,
    notes       VARCHAR(255)  NULL,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT pk_attendance_records PRIMARY KEY (id),
    CONSTRAINT fk_attendance_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE CASCADE,
    INDEX idx_attendance_subject_date (subject_id, class_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE assignments (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    user_id       BIGINT        NOT NULL,
    subject_id    BIGINT        NULL,
    title         VARCHAR(150)  NOT NULL,
    description   TEXT          NULL,
    due_at        DATETIME(6)   NOT NULL,
    priority      ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    status        ENUM('PENDING','IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'PENDING',
    completed_at  DATETIME(6)   NULL,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    CONSTRAINT pk_assignments PRIMARY KEY (id),
    CONSTRAINT fk_assignments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_assignments_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE SET NULL,
    INDEX idx_assignments_user_due (user_id, due_at),
    INDEX idx_assignments_user_status (user_id, status),
    INDEX idx_assignments_user_completed (user_id, completed_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE timetable_entries (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id      BIGINT        NOT NULL,
    subject_id   BIGINT        NOT NULL,
    day_of_week  ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    start_time   TIME          NOT NULL,
    end_time     TIME          NOT NULL,
    location     VARCHAR(100)  NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    CONSTRAINT pk_timetable_entries PRIMARY KEY (id),
    CONSTRAINT fk_timetable_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE CASCADE,
    CONSTRAINT chk_timetable_time_range CHECK (end_time > start_time),
    INDEX idx_timetable_user_day (user_id, day_of_week, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE study_sessions (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    user_id         BIGINT        NOT NULL,
    subject_id      BIGINT        NULL,
    title           VARCHAR(150)  NOT NULL,
    session_date    DATE          NOT NULL,
    start_time      TIME          NOT NULL,
    end_time        TIME          NOT NULL,
    completed       BOOLEAN       NOT NULL DEFAULT FALSE,
    actual_minutes  INT           NULL,
    notes           VARCHAR(500)  NULL,
    completed_at    DATETIME(6)   NULL,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    CONSTRAINT pk_study_sessions PRIMARY KEY (id),
    CONSTRAINT fk_study_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_study_sessions_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE SET NULL,
    CONSTRAINT chk_study_sessions_time_range CHECK (end_time > start_time),
    CONSTRAINT chk_study_sessions_actual CHECK (actual_minutes IS NULL OR actual_minutes BETWEEN 1 AND 1440),
    INDEX idx_study_sessions_user_date (user_id, session_date, start_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE examinations (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    user_id             BIGINT        NOT NULL,
    subject_id          BIGINT        NULL,
    title               VARCHAR(150)  NOT NULL,
    exam_at             DATETIME(6)   NOT NULL,
    duration_minutes    INT           NULL,
    exam_type           ENUM('QUIZ','UNIT_TEST','MIDTERM','FINAL','PRACTICAL','VIVA','OTHER') NOT NULL DEFAULT 'OTHER',
    preparation_status  ENUM('NOT_STARTED','IN_PROGRESS','REVISING','READY') NOT NULL DEFAULT 'NOT_STARTED',
    location            VARCHAR(100)  NULL,
    syllabus_notes      TEXT          NULL,
    created_at          DATETIME(6)   NOT NULL,
    updated_at          DATETIME(6)   NOT NULL,
    CONSTRAINT pk_examinations PRIMARY KEY (id),
    CONSTRAINT fk_examinations_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_examinations_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE SET NULL,
    CONSTRAINT chk_examinations_duration CHECK (duration_minutes IS NULL OR duration_minutes BETWEEN 5 AND 600),
    INDEX idx_examinations_user_exam_at (user_id, exam_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE notes (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    user_id     BIGINT        NOT NULL,
    subject_id  BIGINT        NULL,
    title       VARCHAR(150)  NOT NULL,
    content     MEDIUMTEXT    NOT NULL,
    pinned      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT pk_notes PRIMARY KEY (id),
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notes_subject FOREIGN KEY (subject_id) REFERENCES subjects (id) ON DELETE SET NULL,
    INDEX idx_notes_user_pinned_updated (user_id, pinned, updated_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE note_tags (
    note_id  BIGINT       NOT NULL,
    tag      VARCHAR(30)  NOT NULL,
    CONSTRAINT pk_note_tags PRIMARY KEY (note_id, tag),
    CONSTRAINT fk_note_tags_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    INDEX idx_note_tags_tag (tag)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- Only SHA-256 hashes of tokens are stored, never the tokens themselves.
CREATE TABLE password_reset_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uk_password_reset_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_password_reset_tokens_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    user_id         BIGINT       NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    revoked_at      DATETIME(6)  NULL,
    revoked_reason  ENUM('ROTATED','LOGOUT','PASSWORD_CHANGED','REUSE_DETECTED','ACCOUNT_DISABLED') NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user (user_id, revoked_at),
    INDEX idx_refresh_tokens_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
