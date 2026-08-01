package hu.seregergo.jobsearch.jobapplication.application;

import java.util.UUID;

public class ApplicationActivityNotFoundException extends RuntimeException {

    public ApplicationActivityNotFoundException(
        UUID applicationId,
        UUID activityId
    ) {
        super(
            "Activity " + activityId
                + " was not found for application " + applicationId
        );
    }
}
