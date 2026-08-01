package hu.seregergo.jobsearch.jobapplication.application;

import java.time.LocalDate;
import java.util.UUID;

public record CreateApplicationCommand(
    UUID jobPostingId,
    String nextAction,
    LocalDate dueOn,
    String note
) {
}
