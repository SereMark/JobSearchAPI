package hu.seregergo.jobsearch.jobapplication.application;

import java.util.UUID;

public class JobApplicationNotFoundException extends RuntimeException {

    public JobApplicationNotFoundException(UUID id) {
        super("Application not found: " + id);
    }
}
