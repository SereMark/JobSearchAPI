package hu.seregergo.jobsearch.jobposting.application;

import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;

import java.time.LocalDate;

public record CreateJobPostingCommand(
    String companyName,
    String roleTitle,
    String source,
    String sourceUrl,
    String externalId,
    String location,
    WorkMode workMode,
    LocalDate foundOn,
    JobPostingClassification classification,
    String reviewNote
) {
}
