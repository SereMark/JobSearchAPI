package hu.seregergo.jobsearch.jobapplication.application;

import java.util.UUID;

public class InterviewReportNotFoundException extends RuntimeException {

    public InterviewReportNotFoundException(UUID applicationId, UUID reportId) {
        super(
            "Interview report " + reportId
                + " was not found for application " + applicationId
        );
    }
}
