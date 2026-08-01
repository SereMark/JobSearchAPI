CREATE TABLE applications
(
    id             UUID                     NOT NULL,
    job_posting_id UUID                     NOT NULL,
    submitted_on   DATE,
    stage          VARCHAR(30)              NOT NULL,
    stage_label    VARCHAR(100),
    next_action    VARCHAR(500),
    due_on         DATE,
    outcome        VARCHAR(30),
    note           VARCHAR(2000),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_applications PRIMARY KEY (id),
    CONSTRAINT uq_applications_job_posting UNIQUE (job_posting_id),
    CONSTRAINT fk_applications_job_posting
        FOREIGN KEY (job_posting_id) REFERENCES job_postings (id),
    CONSTRAINT ck_applications_stage
        CHECK (
            stage IN (
                'PREPARING',
                'SUBMITTED',
                'RECRUITER_SCREEN',
                'TECHNICAL_INTERVIEW',
                'TAKE_HOME',
                'HIRING_MANAGER',
                'FINAL',
                'OFFER'
            )
        ),
    CONSTRAINT ck_applications_outcome
        CHECK (
            outcome IS NULL
            OR outcome IN (
                'REJECTED',
                'WITHDRAWN',
                'NO_RESPONSE',
                'ROLE_CANCELLED',
                'OFFER_DECLINED',
                'SIGNED'
            )
        ),
    CONSTRAINT ck_applications_stage_label_not_blank
        CHECK (stage_label IS NULL OR stage_label ~ '[^[:space:]]'),
    CONSTRAINT ck_applications_next_action_not_blank
        CHECK (next_action IS NULL OR next_action ~ '[^[:space:]]'),
    CONSTRAINT ck_applications_note_not_blank
        CHECK (note IS NULL OR note ~ '[^[:space:]]'),
    CONSTRAINT ck_applications_active_work
        CHECK (
            (outcome IS NULL AND next_action IS NOT NULL AND due_on IS NOT NULL)
            OR
            (outcome IS NOT NULL AND next_action IS NULL AND due_on IS NULL)
        ),
    CONSTRAINT ck_applications_submission_state
        CHECK (
            (stage = 'PREPARING' AND submitted_on IS NULL)
            OR
            (stage <> 'PREPARING' AND submitted_on IS NOT NULL)
        ),
    CONSTRAINT ck_applications_preparing_outcome
        CHECK (
            stage <> 'PREPARING'
            OR outcome IS NULL
            OR outcome IN ('WITHDRAWN', 'ROLE_CANCELLED')
        ),
    CONSTRAINT ck_applications_offer_outcome
        CHECK (
            outcome NOT IN ('OFFER_DECLINED', 'SIGNED')
            OR stage = 'OFFER'
        ),
    CONSTRAINT ck_applications_updated_at_not_before_created_at
        CHECK (updated_at >= created_at)
);
