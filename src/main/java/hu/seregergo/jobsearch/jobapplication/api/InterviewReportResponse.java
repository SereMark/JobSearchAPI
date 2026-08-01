package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.domain.InterviewReport;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record InterviewReportResponse(
    UUID id,
    UUID applicationId,
    LocalDate interviewedOn,
    String roundLabel,
    String report,
    Instant createdAt,
    Instant updatedAt
) {

    public static InterviewReportResponse from(InterviewReport report) {
        return new InterviewReportResponse(
            report.getId(),
            report.getApplication().getId(),
            report.getInterviewedOn(),
            report.getRoundLabel(),
            report.getReport(),
            report.getCreatedAt(),
            report.getUpdatedAt()
        );
    }
}
