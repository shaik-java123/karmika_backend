-- ============================================================
-- Fix: Convert VARCHAR → ENUM for Onboarding tables
-- Hibernate 6 maps @Enumerated(EnumType.STRING) to MySQL ENUM
-- Run this, then restart the backend
-- ============================================================

USE karmika_hrms;

-- ── onboarding_checklists ────────────────────────────────────

ALTER TABLE onboarding_checklists
    MODIFY COLUMN status
        ENUM('PENDING','IN_REVIEW','APPROVED','REJECTED','WAIVED')
        NOT NULL DEFAULT 'PENDING';

ALTER TABLE onboarding_checklists
    MODIFY COLUMN task_type
        ENUM('UPLOAD_PHOTO','UPLOAD_DOCUMENT','FILL_FORM','SIGN_DOCUMENT','GENERAL','ACKNOWLEDGEMENT')
        NOT NULL DEFAULT 'GENERAL';

-- ── onboarding_documents ─────────────────────────────────────

ALTER TABLE onboarding_documents
    MODIFY COLUMN source
        ENUM('HR_UPLOAD','EMPLOYEE_UPLOAD')
        NOT NULL DEFAULT 'HR_UPLOAD';

ALTER TABLE onboarding_documents
    MODIFY COLUMN document_type
        ENUM('OFFER_LETTER','APPOINTMENT_LETTER','NDA','POLICY_DOCUMENT','HANDBOOK',
             'ID_PROOF','ADDRESS_PROOF','EDUCATIONAL_CERTIFICATE','EXPERIENCE_LETTER',
             'PAN_CARD','AADHAAR_CARD','PASSPORT','BANK_DETAILS','PHOTO','OTHER')
        NULL;

SELECT 'ENUM columns fixed!' AS status;
