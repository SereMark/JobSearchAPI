ALTER TABLE job_postings
    ADD COLUMN target_track VARCHAR(10),
    ADD COLUMN description_snapshot VARCHAR(50000),
    ADD COLUMN updated_at TIMESTAMP WITH TIME ZONE;

UPDATE job_postings
SET target_track = 'JAVA',
    updated_at = created_at;

ALTER TABLE job_postings
    ALTER COLUMN target_track SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT ck_job_postings_target_track
        CHECK (target_track IN ('JAVA', 'DOTNET')),
    ADD CONSTRAINT ck_job_postings_description_snapshot_not_blank
        CHECK (
            description_snapshot IS NULL
            OR description_snapshot ~ '[^[:space:]]'
        ),
    ADD CONSTRAINT ck_job_postings_updated_at_not_before_created_at
        CHECK (updated_at >= created_at);
