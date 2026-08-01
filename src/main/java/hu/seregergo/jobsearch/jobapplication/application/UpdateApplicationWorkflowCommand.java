package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;

import java.time.LocalDate;

public record UpdateApplicationWorkflowCommand(
    ApplicationStage stage,
    String stageLabel,
    String nextAction,
    LocalDate dueOn,
    ApplicationOutcome outcome,
    String note
) {
}
