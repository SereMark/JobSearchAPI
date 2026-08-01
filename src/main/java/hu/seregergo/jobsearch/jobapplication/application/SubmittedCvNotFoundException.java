package hu.seregergo.jobsearch.jobapplication.application;

import java.util.UUID;

public class SubmittedCvNotFoundException extends RuntimeException {

    public SubmittedCvNotFoundException(UUID applicationId) {
        super("No submitted CV was recorded for application " + applicationId);
    }
}
