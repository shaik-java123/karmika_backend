-- ============================================================
-- Karmika HRMS — Combined Migration
-- Run this against karmika_hrms BEFORE starting the backend
-- ============================================================

USE karmika_hrms;

-- ────────────────────────────────────────────────────────────
-- 1. Password Reset columns (for forgot-password feature)
-- ────────────────────────────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS reset_token        VARCHAR(10) NULL,
    ADD COLUMN IF NOT EXISTS reset_token_expiry DATETIME    NULL;

-- ────────────────────────────────────────────────────────────
-- 2. Onboarding tables
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS onboarding_checklists (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id        BIGINT        NOT NULL,
    assigned_by_id     BIGINT,
    title              VARCHAR(255)  NOT NULL,
    description        VARCHAR(1000),
    task_type          VARCHAR(50)   NOT NULL DEFAULT 'GENERAL',
    status             VARCHAR(50)   NOT NULL DEFAULT 'PENDING',
    due_date           DATE,
    hr_notes           VARCHAR(1000),
    employee_notes     VARCHAR(1000),
    attachment_path    VARCHAR(500),
    attachment_name    VARCHAR(255),
    rejection_reason   VARCHAR(500),
    completed_at       DATETIME,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME               DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_oc_employee   FOREIGN KEY (employee_id)    REFERENCES employees(id),
    CONSTRAINT fk_oc_assigned   FOREIGN KEY (assigned_by_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS onboarding_documents (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id        BIGINT        NOT NULL,
    uploaded_by_id     BIGINT,
    source             VARCHAR(50)   NOT NULL DEFAULT 'HR_UPLOAD',
    document_name      VARCHAR(255)  NOT NULL,
    description        VARCHAR(500),
    document_type      VARCHAR(100),
    file_path          VARCHAR(500)  NOT NULL,
    original_file_name VARCHAR(255),
    mime_type          VARCHAR(100),
    file_size          BIGINT,
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_od_employee   FOREIGN KEY (employee_id)    REFERENCES employees(id),
    CONSTRAINT fk_od_uploader   FOREIGN KEY (uploaded_by_id) REFERENCES employees(id)
);

-- ────────────────────────────────────────────────────────────
-- 3. Fix utf8mb4 for emoji support in notifications
--    (MySQL default utf8 only handles 3-byte chars; emojis are 4-byte)
-- ────────────────────────────────────────────────────────────
ALTER TABLE notifications
    CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE notifications
    MODIFY COLUMN title   VARCHAR(255)  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    MODIFY COLUMN message VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ────────────────────────────────────────────────────────────
-- 4. Expand notifications.type ENUM with new onboarding types
--    Error: "Data truncated for column 'type'" = value not in ENUM
-- ────────────────────────────────────────────────────────────
ALTER TABLE notifications
    MODIFY COLUMN type ENUM(
        'LEAVE_APPROVED',
        'LEAVE_REJECTED',
        'LEAVE_APPLIED',
        'LEAVE_CANCELLED',
        'ATTENDANCE_REMINDER',
        'SYSTEM_ALERT',
        'TASK_ASSIGNED',
        'TASK_COMPLETED',
        'ANNOUNCEMENT',
        'INFO',
        'ONBOARDING_STARTED',
        'ONBOARDING_TASK_SUBMITTED',
        'ONBOARDING_TASK_APPROVED',
        'ONBOARDING_TASK_REJECTED',
        'ONBOARDING_TASK_WAIVED',
        'ONBOARDING_DOCUMENT_SHARED'
    ) NOT NULL;

-- ────────────────────────────────────────────────────────────
-- Verify
-- ────────────────────────────────────────────────────────────
SELECT 'Migration complete!' AS status;
SHOW COLUMNS FROM users LIKE 'reset%';
SHOW TABLES LIKE 'onboarding%';
SHOW FULL COLUMNS FROM notifications LIKE 'title';
SHOW FULL COLUMNS FROM notifications LIKE 'type';

-- ────────────────────────────────────────────────────────────
-- Goals & Targets table
-- ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS goals (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id      BIGINT         NOT NULL,
    assigned_by_id   BIGINT,
    cycle_id         BIGINT,

    title            VARCHAR(300)   NOT NULL,
    description      TEXT,
    target_metric    VARCHAR(500),
    target_value     DOUBLE,
    achieved_value   DOUBLE,
    progress_pct     INT            NOT NULL DEFAULT 0,
    weightage        INT            NOT NULL DEFAULT 10,

    category         VARCHAR(50)    NOT NULL DEFAULT 'PERFORMANCE',
    priority         VARCHAR(50)    NOT NULL DEFAULT 'MEDIUM',
    status           VARCHAR(50)    NOT NULL DEFAULT 'NOT_STARTED',

    due_date         DATE           NOT NULL,
    completed_date   DATE,

    manager_comments VARCHAR(1000),
    self_comments    VARCHAR(1000),

    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_goal_employee FOREIGN KEY (employee_id)    REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT fk_goal_creator  FOREIGN KEY (assigned_by_id) REFERENCES employees(id) ON DELETE SET NULL,
    CONSTRAINT fk_goal_cycle    FOREIGN KEY (cycle_id)       REFERENCES appraisal_cycles(id) ON DELETE SET NULL
);

-- Useful indexes
CREATE INDEX IF NOT EXISTS idx_goals_employee  ON goals (employee_id);
CREATE INDEX IF NOT EXISTS idx_goals_cycle     ON goals (cycle_id);
CREATE INDEX IF NOT EXISTS idx_goals_status    ON goals (status);
CREATE INDEX IF NOT EXISTS idx_goals_due       ON goals (due_date);

SHOW TABLES LIKE 'goals';

