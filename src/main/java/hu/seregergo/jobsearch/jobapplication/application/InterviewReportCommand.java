package hu.seregergo.jobsearch.jobapplication.application;

import java.time.LocalDate;

public record InterviewReportCommand(
    LocalDate interviewedOn,
    String roundLabel,
    String report
) {
}
