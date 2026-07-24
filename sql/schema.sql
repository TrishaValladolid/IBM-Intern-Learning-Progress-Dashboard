-- ============================================================
--  Learning Progress Dashboard - Database Schema
--  PostgreSQL 14+
--
--  Run this file first, then run seed.sql.
--
--  Usage:
--    psql -U postgres -d progress_dashboard -f schema.sql
-- ============================================================

-- Drop tables in reverse dependency order so re-running is safe.
DROP TABLE IF EXISTS user_training_assignment CASCADE;
DROP TABLE IF EXISTS submission            CASCADE;
DROP TABLE IF EXISTS attendance            CASCADE;
DROP TABLE IF EXISTS attendance_session    CASCADE;
DROP TABLE IF EXISTS training              CASCADE;
DROP TABLE IF EXISTS assignment            CASCADE;
DROP TABLE IF EXISTS intern                CASCADE;
DROP TABLE IF EXISTS app_user              CASCADE;

-- ============================================================
--  app_user
--  Application accounts. Mapped from User.java.
--  "user" is a reserved word in PostgreSQL, so the table is app_user.
-- ============================================================
CREATE TABLE app_user (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(512) NOT NULL,          -- PBKDF2 hash: "iterations:salt:hash"
    role          VARCHAR(20)  NOT NULL,           -- ADMIN | TRAINER
    full_name     VARCHAR(255),
    email         VARCHAR(255),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date  TIMESTAMP
);

-- ============================================================
--  user_training_assignment
--  Which training areas a trainer is restricted to.
--  Empty = no restriction (trainer sees all assignments).
--  Mapped from User.assignedTrainings (@ElementCollection).
-- ============================================================
CREATE TABLE user_training_assignment (
    user_id       BIGINT       NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    training_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (user_id, training_name)
);

-- ============================================================
--  intern
--  Mapped from Intern.java.
-- ============================================================
CREATE TABLE intern (
    id                           BIGSERIAL    PRIMARY KEY,
    name                         VARCHAR(255) NOT NULL,
    talent_id                    VARCHAR(255),
    batch                        VARCHAR(255),
    track                        VARCHAR(255),
    status                       VARCHAR(30)  DEFAULT 'ACTIVE',  -- ACTIVE | OFFBOARDED | CONVERTED_TO_EMPLOYEE | WITHDRAWN | TERMINATED
    total_hours_required         DOUBLE PRECISION,
    expected_graduation_date     VARCHAR(20),   -- ISO yyyy-MM-dd
    expected_internship_end_date VARCHAR(20),   -- ISO yyyy-MM-dd
    school                       VARCHAR(255),
    course                       VARCHAR(255)
);

-- ============================================================
--  assignment
--  Mapped from Assignment.java.
-- ============================================================
CREATE TABLE assignment (
    id            BIGSERIAL    PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    max_score     INTEGER,
    batch         VARCHAR(255),
    training_name VARCHAR(255),  -- links to the training area (free text)
    repo_url      VARCHAR(1000), -- optional Box Drive or GitHub link
    due_date      VARCHAR(20)    -- ISO yyyy-MM-dd
);

-- ============================================================
--  training
--  One row per training an intern has completed.
--  Mapped from Training.java.
-- ============================================================
CREATE TABLE training (
    id            BIGSERIAL    PRIMARY KEY,
    intern_id     BIGINT       NOT NULL REFERENCES intern(id) ON DELETE CASCADE,
    training_name VARCHAR(255) NOT NULL,
    repo_url      VARCHAR(1000) -- optional GitHub / Box Drive link
);

-- ============================================================
--  submission
--  One row per intern per assignment (the grade record).
--  Mapped from Submission.java.
-- ============================================================
CREATE TABLE submission (
    id            BIGSERIAL   PRIMARY KEY,
    intern_id     BIGINT      NOT NULL REFERENCES intern(id)     ON DELETE CASCADE,
    assignment_id BIGINT      NOT NULL REFERENCES assignment(id) ON DELETE CASCADE,
    score         INTEGER,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'  -- PENDING | SUBMITTED | GRADED
);

-- ============================================================
--  attendance_session
--  Groups all attendance records for one training on one date.
--  Unique (batch, session_date) prevents duplicate sessions.
--  Mapped from AttendanceSession.java.
-- ============================================================
CREATE TABLE attendance_session (
    id            BIGSERIAL   PRIMARY KEY,
    batch         VARCHAR(255) NOT NULL,
    session_date  DATE         NOT NULL,
    recorded_by   VARCHAR(255),
    created_date  TIMESTAMP,
    UNIQUE (batch, session_date)
);

-- ============================================================
--  attendance
--  One row per intern per date.
--  session_id is nullable so records created before sessions
--  were introduced still load correctly.
--  Mapped from Attendance.java.
-- ============================================================
CREATE TABLE attendance (
    id              BIGSERIAL   PRIMARY KEY,
    intern_id       BIGINT      NOT NULL REFERENCES intern(id)              ON DELETE CASCADE,
    attendance_date DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL,  -- PRESENT | LATE | ABSENT
    recorded_by     VARCHAR(255),
    session_id      BIGINT      REFERENCES attendance_session(id) ON DELETE SET NULL,
    created_date    TIMESTAMP
);
