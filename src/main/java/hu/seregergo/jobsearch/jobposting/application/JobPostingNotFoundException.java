package hu.seregergo.jobsearch.jobposting.application;

import java.util.UUID;

public class JobPostingNotFoundException extends RuntimeException {

    public JobPostingNotFoundException(UUID id) {
        super("Job posting with id " + id + " was not found");
    }
}
