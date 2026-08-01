package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.SubmitApplicationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Submission details for a prepared application")
public record SubmitApplicationRequest(
    @Schema(example = "2026-08-01")
    @NotNull(message = "Submission date is required")
    @PastOrPresent(message = "Submission date must not be in the future")
    LocalDate submittedOn,

    @Schema(example = "Check for a response")
    @NotBlank(message = "Next action is required")
    @Size(max = 500, message = "Next action must not exceed 500 characters")
    String nextAction,

    @Schema(example = "2026-08-08")
    @NotNull(message = "Due date is required")
    LocalDate dueOn
) {

    public SubmitApplicationCommand toCommand() {
        return new SubmitApplicationCommand(submittedOn, nextAction, dueOn);
    }
}
