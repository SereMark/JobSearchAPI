package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.api.validation.ValidApplicationWorkflow;
import hu.seregergo.jobsearch.jobapplication.application.UpdateApplicationWorkflowCommand;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@ValidApplicationWorkflow
@Schema(description = "Complete replacement of an application's workflow fields")
public record UpdateApplicationWorkflowRequest(
    @Schema(example = "TECHNICAL_INTERVIEW")
    @NotNull(message = "Stage is required")
    ApplicationStage stage,

    @Schema(description = "Optional source-specific name for the current stage")
    @Size(max = 100, message = "Stage label must not exceed 100 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Stage label must not be blank")
    String stageLabel,

    @Schema(
        description = "Required while active and omitted when closed",
        example = "Prepare examples for the technical interview"
    )
    @Size(max = 500, message = "Next action must not exceed 500 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Next action must not be blank")
    String nextAction,

    @Schema(
        description = "Required while active and omitted when closed",
        example = "2026-08-08"
    )
    LocalDate dueOn,

    @Schema(description = "Null while active; a value closes the application")
    ApplicationOutcome outcome,

    @Schema(example = "Technical interview scheduled with two engineers")
    @Size(max = 2000, message = "Note must not exceed 2,000 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Note must not be blank")
    String note
) {

    public UpdateApplicationWorkflowCommand toCommand() {
        return new UpdateApplicationWorkflowCommand(
            stage,
            stageLabel,
            nextAction,
            dueOn,
            outcome,
            note
        );
    }
}
