CREATE TABLE application_activities
(
    id               UUID                     NOT NULL,
    application_id   UUID                     NOT NULL,
    occurred_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    activity_type    VARCHAR(32)              NOT NULL,
    summary          VARCHAR(500)             NOT NULL,
    details          VARCHAR(5000),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT pk_application_activities PRIMARY KEY (id),
    CONSTRAINT fk_application_activities_application
        FOREIGN KEY (application_id) REFERENCES applications (id)
            ON DELETE CASCADE,
    CONSTRAINT ck_application_activities_type
        CHECK (activity_type IN (
            'EMAIL',
            'CALL',
            'LINKEDIN',
            'FOLLOW_UP',
            'TASK',
            'OTHER'
        )),
    CONSTRAINT ck_application_activities_summary_not_blank
        CHECK (summary ~ '[^[:space:]]'),
    CONSTRAINT ck_application_activities_details_not_blank
        CHECK (details IS NULL OR details ~ '[^[:space:]]'),
    CONSTRAINT ck_application_activities_occurred_before_update
        CHECK (occurred_at <= updated_at),
    CONSTRAINT ck_application_activities_updated_after_creation
        CHECK (updated_at >= created_at)
);

CREATE INDEX ix_application_activities_timeline
    ON application_activities (
        application_id,
        occurred_at DESC,
        created_at DESC,
        id DESC
    );
