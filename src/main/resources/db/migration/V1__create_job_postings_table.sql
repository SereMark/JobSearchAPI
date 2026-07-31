CREATE TABLE job_postings
(
    id             UUID                     NOT NULL,
    company_name   VARCHAR(200)             NOT NULL,
    role_title     VARCHAR(200)             NOT NULL,
    source         VARCHAR(100)             NOT NULL,
    source_url     VARCHAR(2048),
    external_id    VARCHAR(200),
    location       VARCHAR(200),
    work_mode      VARCHAR(20)              NOT NULL,
    found_on       DATE                     NOT NULL,
    classification CHAR(1)                  NOT NULL,
    review_note    VARCHAR(1000),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_job_postings PRIMARY KEY (id),
    CONSTRAINT ck_job_postings_company_name_not_blank
        CHECK (company_name ~ '[^[:space:]]'),
    CONSTRAINT ck_job_postings_role_title_not_blank
        CHECK (role_title ~ '[^[:space:]]'),
    CONSTRAINT ck_job_postings_source_not_blank
        CHECK (source ~ '[^[:space:]]'),
    CONSTRAINT ck_job_postings_source_url_http
        CHECK (source_url IS NULL OR source_url ~* '^https?://[^[:space:]]+$'),
    CONSTRAINT ck_job_postings_external_id_not_blank
        CHECK (external_id IS NULL OR external_id ~ '[^[:space:]]'),
    CONSTRAINT ck_job_postings_source_reference_present
        CHECK (source_url IS NOT NULL OR external_id IS NOT NULL),
    CONSTRAINT ck_job_postings_location_not_blank
        CHECK (location IS NULL OR location ~ '[^[:space:]]'),
    CONSTRAINT ck_job_postings_work_mode
        CHECK (work_mode IN ('ONSITE', 'HYBRID', 'REMOTE', 'UNKNOWN')),
    CONSTRAINT ck_job_postings_classification
        CHECK (classification IN ('A', 'B', 'C')),
    CONSTRAINT ck_job_postings_review_note_not_blank
        CHECK (review_note IS NULL OR review_note ~ '[^[:space:]]'),
    CONSTRAINT ck_job_postings_c_classification_has_note
        CHECK (
            classification <> 'C'
            OR (review_note IS NOT NULL AND review_note ~ '[^[:space:]]')
        )
);
