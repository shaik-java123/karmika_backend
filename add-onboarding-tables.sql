-- ============================================================
-- Migration: Create onboarding tables
-- Run BEFORE restarting the backend (ddl-auto: validate)
-- ============================================================

CREATE TABLE IF NOT EXISTS onboarding_checklists (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id        BIGINT NOT NULL,
    assigned_by_id     BIGINT,
    title              VARCHAR(255) NOT NULL,
    description        VARCHAR(1000),
    task_type          VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    status             VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    due_date           DATE,
    hr_notes           VARCHAR(1000),
    employee_notes     VARCHAR(1000),
    attachment_path    VARCHAR(500),
    attachment_name    VARCHAR(255),
    rejection_reason   VARCHAR(500),
    completed_at       DATETIME,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_oc_employee   FOREIGN KEY (employee_id)    REFERENCES employees(id),
    CONSTRAINT fk_oc_assigned   FOREIGN KEY (assigned_by_id) REFERENCES employees(id)
);

CREATE TABLE IF NOT EXISTS onboarding_documents (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id        BIGINT NOT NULL,
    uploaded_by_id     BIGINT,
    source             VARCHAR(50) NOT NULL DEFAULT 'HR_UPLOAD',
    document_name      VARCHAR(255) NOT NULL,
    description        VARCHAR(500),
    document_type      VARCHAR(100),
    file_path          VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255),
    mime_type          VARCHAR(100),
    file_size          BIGINT,
    created_at         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_od_employee   FOREIGN KEY (employee_id)    REFERENCES employees(id),
    CONSTRAINT fk_od_uploader   FOREIGN KEY (uploaded_by_id) REFERENCES employees(id)
);

SELECT 'Onboarding tables created successfully.' AS status;
