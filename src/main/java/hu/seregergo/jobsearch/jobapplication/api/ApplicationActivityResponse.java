package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivity;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivityType;

import java.time.Instant;
import java.util.UUID;

public record ApplicationActivityResponse(
    UUID id,
    UUID applicationId,
    Instant occurredAt,
    ApplicationActivityType type,
    String summary,
    String details,
    Instant createdAt,
    Instant updatedAt
) {

    public static ApplicationActivityResponse from(ApplicationActivity activity) {
        return new ApplicationActivityResponse(
            activity.getId(),
            activity.getApplication().getId(),
            activity.getOccurredAt(),
            activity.getType(),
            activity.getSummary(),
            activity.getDetails(),
            activity.getCreatedAt(),
            activity.getUpdatedAt()
        );
    }
}
