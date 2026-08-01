package hu.seregergo.jobsearch.jobapplication.persistence;

import java.time.Instant;
import java.util.UUID;

public interface ApplicationLastActivityProjection {

    UUID getApplicationId();

    Instant getLastActivityAt();
}
