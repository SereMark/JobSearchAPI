package hu.seregergo.jobsearch.jobposting.application;

import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;

import java.time.LocalDate;

public record UpdateJobPostingCommand(
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
    String descriptionSnapshot
) {
}
