CREATE TABLE submitted_cvs (
    application_id UUID PRIMARY KEY
        REFERENCES applications (id),
    sent_on DATE NOT NULL,
    language VARCHAR(2) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    bytes BYTEA NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT submitted_cvs_language_check
        CHECK (language IN ('HU', 'EN')),
    CONSTRAINT submitted_cvs_file_name_check
        CHECK (
            btrim(original_file_name) <> ''
            AND position('/' IN original_file_name) = 0
            AND position(chr(92) IN original_file_name) = 0
        ),
    CONSTRAINT submitted_cvs_size_check
        CHECK (size_bytes BETWEEN 1 AND 5242880),
    CONSTRAINT submitted_cvs_size_matches_bytes_check
        CHECK (octet_length(bytes) = size_bytes),
    CONSTRAINT submitted_cvs_pdf_header_check
        CHECK (substring(bytes FROM 1 FOR 5) = decode('255044462d', 'hex')),
    CONSTRAINT submitted_cvs_sha256_check
        CHECK (sha256 ~ '^[0-9a-f]{64}$')
);

CREATE TABLE application_idempotency_records (
    idempotency_key UUID PRIMARY KEY,
    application_id UUID NOT NULL
        REFERENCES applications (id),
    operation VARCHAR(30) NOT NULL,
    request_fingerprint VARCHAR(64) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT application_idempotency_operation_check
        CHECK (operation IN ('SUBMIT', 'RECORD_SENT_CV')),
    CONSTRAINT application_idempotency_fingerprint_check
        CHECK (request_fingerprint ~ '^[0-9a-f]{64}$'),
    CONSTRAINT application_idempotency_status_check
        CHECK (response_status BETWEEN 200 AND 299),
    CONSTRAINT application_idempotency_response_check
        CHECK (btrim(response_body) <> ''),
    CONSTRAINT application_idempotency_operation_unique
        UNIQUE (application_id, operation)
);
