package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;

import java.time.Instant;
import java.util.Objects;

public record JobApplicationDetails(
    JobApplication application,
    SubmittedCvMetadata submittedCv,
    Instant lastActivityAt
) {

    public JobApplicationDetails {
        Objects.requireNonNull(application, "application must not be null");
    }
}
