package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ApplicationSubmissionReceipt(
    UUID applicationId,
    ApplicationStage stage,
    LocalDate submittedOn,
    String nextAction,
    LocalDate dueOn,
    Instant updatedAt,
    SubmittedCvMetadata submittedCv
) {
}
