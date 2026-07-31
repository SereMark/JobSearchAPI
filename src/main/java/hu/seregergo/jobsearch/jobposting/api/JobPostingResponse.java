package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record JobPostingResponse(
    UUID id,
    String companyName,
    String roleTitle,
    String source,
    String sourceUrl,
    String externalId,
    String location,
    WorkMode workMode,
    LocalDate foundOn,
    JobPostingClassification classification,
    String reviewNote,
    Instant createdAt
) {

    public static JobPostingResponse from(JobPosting jobPosting) {
        return new JobPostingResponse(
            jobPosting.getId(),
            jobPosting.getCompanyName(),
            jobPosting.getRoleTitle(),
            jobPosting.getSource(),
            jobPosting.getSourceUrl(),
            jobPosting.getExternalId(),
            jobPosting.getLocation(),
            jobPosting.getWorkMode(),
            jobPosting.getFoundOn(),
            jobPosting.getClassification(),
            jobPosting.getReviewNote(),
            jobPosting.getCreatedAt()
        );
    }
}
