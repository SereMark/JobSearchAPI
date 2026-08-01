package hu.seregergo.jobsearch.jobapplication.application;

import java.util.Objects;

public record StoredOperationResponse(int status, String body) {

    public StoredOperationResponse {
        if (status < 200 || status > 299) {
            throw new IllegalArgumentException("status must be successful");
        }
        Objects.requireNonNull(body, "body must not be null");
    }
}
