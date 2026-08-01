package hu.seregergo.jobsearch.jobapplication.application;

import java.time.LocalDate;

public record SubmitApplicationCommand(
    LocalDate submittedOn,
    String nextAction,
    LocalDate dueOn
) {
}
