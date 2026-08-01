package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.InterviewReportCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Fields for creating or replacing an interview report")
public record InterviewReportRequest(
    @Schema(example = "2026-07-30")
    @NotNull(message = "Interview date is required")
    @PastOrPresent(message = "Interview date must not be in the future")
    LocalDate interviewedOn,

    @Schema(example = "Technical interview - round 1")
    @NotBlank(message = "Round label is required")
    @Size(max = 200, message = "Round label must not exceed 200 characters")
    String roundLabel,

    @Schema(
        description = "Free-form reflection, questions, observations, and lessons",
        example = "The architecture discussion went well. Revisit transaction isolation."
    )
    @NotBlank(message = "Interview report is required")
    @Size(
        max = 20_000,
        message = "Interview report must not exceed 20,000 characters"
    )
    String report
) {

    public InterviewReportCommand toCommand() {
        return new InterviewReportCommand(interviewedOn, roundLabel, report);
    }
}
