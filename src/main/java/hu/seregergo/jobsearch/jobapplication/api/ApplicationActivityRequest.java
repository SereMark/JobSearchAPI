package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.ApplicationActivityCommand;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivityType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@Schema(description = "Fields for creating or replacing an application activity")
public record ApplicationActivityRequest(
    @Schema(example = "2026-07-31T14:30:00Z")
    @NotNull(message = "Activity time is required")
    @PastOrPresent(message = "Activity time must not be in the future")
    Instant occurredAt,

    @Schema(example = "EMAIL")
    @NotNull(message = "Activity type is required")
    ApplicationActivityType type,

    @Schema(example = "Recruiter confirmed the technical interview")
    @NotBlank(message = "Activity summary is required")
    @Size(max = 500, message = "Activity summary must not exceed 500 characters")
    String summary,

    @Schema(
        description = "Optional context that should remain in the application history",
        example = "The interview will focus on Java, Spring, and system design."
    )
    @Size(max = 5_000, message = "Activity details must not exceed 5,000 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Activity details must not be blank")
    String details
) {

    public ApplicationActivityCommand toCommand() {
        return new ApplicationActivityCommand(occurredAt, type, summary, details);
    }
}
