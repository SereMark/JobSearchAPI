package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;

public interface JobPostingRequest {

    String sourceUrl();

    String externalId();

    JobPostingClassification classification();

    String reviewNote();
}
