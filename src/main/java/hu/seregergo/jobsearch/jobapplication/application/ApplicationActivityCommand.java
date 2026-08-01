package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivityType;

import java.time.Instant;

public record ApplicationActivityCommand(
    Instant occurredAt,
    ApplicationActivityType type,
    String summary,
    String details
) {
}
