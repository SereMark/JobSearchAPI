CREATE TABLE interview_reports
(
    id               UUID                     NOT NULL,
    application_id   UUID                     NOT NULL,
    interviewed_on   DATE                     NOT NULL,
    round_label      VARCHAR(200)             NOT NULL,
    report           VARCHAR(20000)           NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_interview_reports PRIMARY KEY (id),
    CONSTRAINT fk_interview_reports_application
        FOREIGN KEY (application_id) REFERENCES applications (id)
            ON DELETE CASCADE,
    CONSTRAINT ck_interview_reports_round_label_not_blank
        CHECK (round_label ~ '[^[:space:]]'),
    CONSTRAINT ck_interview_reports_report_not_blank
        CHECK (report ~ '[^[:space:]]'),
    CONSTRAINT ck_interview_reports_updated_at_not_before_created_at
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_interview_reports_application_timeline
    ON interview_reports (
        application_id,
        interviewed_on DESC,
        created_at DESC,
        id DESC
    );
