package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobPostingSummaryResponse(
    UUID id,
    String companyName,
    String roleTitle,
    String source,
    String sourceUrl,
    String externalId,
    String location,
    WorkMode workMode,
    LocalDate foundOn,
    TargetTrack targetTrack,
    JobPostingClassification classification,
    String reviewNote,
    boolean hasDescriptionSnapshot,
    Instant createdAt,
    Instant updatedAt
) {

    public static JobPostingSummaryResponse from(JobPosting jobPosting) {
        return new JobPostingSummaryResponse(
            jobPosting.getId(),
            jobPosting.getCompanyName(),
            jobPosting.getRoleTitle(),
            jobPosting.getSource(),
            jobPosting.getSourceUrl(),
            jobPosting.getExternalId(),
            jobPosting.getLocation(),
            jobPosting.getWorkMode(),
            jobPosting.getFoundOn(),
            jobPosting.getTargetTrack(),
            jobPosting.getClassification(),
            jobPosting.getReviewNote(),
            jobPosting.getDescriptionSnapshot() != null,
            jobPosting.getCreatedAt(),
            jobPosting.getUpdatedAt()
        );
    }
}
