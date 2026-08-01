package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.CreateApplicationCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Preparation work for one tracked job posting")
public record CreateApplicationRequest(
    @Schema(example = "b02385a1-bc9b-4a91-85c6-64d3fb82f040")
    @NotNull(message = "Job posting ID is required")
    UUID jobPostingId,

    @Schema(example = "Tailor the CV for the role")
    @NotBlank(message = "Next action is required")
    @Size(max = 500, message = "Next action must not exceed 500 characters")
    String nextAction,

    @Schema(example = "2026-08-04")
    @NotNull(message = "Due date is required")
    LocalDate dueOn,

    @Schema(example = "Emphasize recent Spring Boot work")
    @Size(max = 2000, message = "Note must not exceed 2,000 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Note must not be blank")
    String note
) {

    public CreateApplicationCommand toCommand() {
        return new CreateApplicationCommand(jobPostingId, nextAction, dueOn, note);
    }
}
