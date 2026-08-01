package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.JobApplicationDetails;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvMetadata;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobApplicationResponse(
    UUID id,
    UUID jobPostingId,
    TargetTrack targetTrack,
    String companyName,
    String roleTitle,
    LocalDate submittedOn,
    ApplicationStage stage,
    String stageLabel,
    String nextAction,
    LocalDate dueOn,
    ApplicationOutcome outcome,
    String note,
    boolean active,
    Instant createdAt,
    Instant updatedAt,
    @Schema(description = "Occurrence time of the application's latest activity")
    Instant lastActivityAt,
    SubmittedCvMetadata submittedCv
) {

    public static JobApplicationResponse from(JobApplicationDetails details) {
        JobApplication application = details.application();
        JobPosting jobPosting = application.getJobPosting();
        return new JobApplicationResponse(
            application.getId(),
            jobPosting.getId(),
            jobPosting.getTargetTrack(),
            jobPosting.getCompanyName(),
            jobPosting.getRoleTitle(),
            application.getSubmittedOn(),
            application.getStage(),
            application.getStageLabel(),
            application.getNextAction(),
            application.getDueOn(),
            application.getOutcome(),
            application.getNote(),
            application.isActive(),
            application.getCreatedAt(),
            application.getUpdatedAt(),
            details.lastActivityAt(),
            details.submittedCv()
        );
    }
}
