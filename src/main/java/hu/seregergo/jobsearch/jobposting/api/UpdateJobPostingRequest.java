package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.api.validation.ValidJobPostingRequest;
import hu.seregergo.jobsearch.jobposting.application.UpdateJobPostingCommand;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@ValidJobPostingRequest
@Schema(description = "Complete replacement for a job posting's editable fields")
public record UpdateJobPostingRequest(
    @Schema(example = "Example Technologies Kft.")
    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must not exceed 200 characters")
    String companyName,

    @Schema(example = "Java Backend Developer")
    @NotBlank(message = "Role title is required")
    @Size(max = 200, message = "Role title must not exceed 200 characters")
    String roleTitle,

    @Schema(example = "Company careers")
    @NotBlank(message = "Source is required")
    @Size(max = 100, message = "Source must not exceed 100 characters")
    String source,

    @Schema(
        description = "HTTP or HTTPS link; sourceUrl or externalId must be present",
        example = "https://careers.example.com/jobs/123"
    )
    @Size(max = 2048, message = "Source URL must not exceed 2048 characters")
    @URL(
        regexp = "(?i)^https?://.*$",
        message = "Source URL must be a valid HTTP or HTTPS URL"
    )
    @Pattern(regexp = "(?s).*\\S.*", message = "Source URL must not be blank")
    String sourceUrl,

    @Schema(
        description = "Identifier assigned by the source; sourceUrl or externalId must be present",
        example = "JOB-123"
    )
    @Size(max = 200, message = "External ID must not exceed 200 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "External ID must not be blank")
    String externalId,

    @Schema(example = "Budapest")
    @Size(max = 200, message = "Location must not exceed 200 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Location must not be blank")
    String location,

    @Schema(example = "HYBRID")
    @NotNull(message = "Work mode is required")
    WorkMode workMode,

    @Schema(example = "2026-07-30")
    @NotNull(message = "Found date is required")
    @PastOrPresent(message = "Found date must not be in the future")
    LocalDate foundOn,

    @Schema(description = "Primary technology track for this opportunity", example = "JAVA")
    @NotNull(message = "Target track is required")
    TargetTrack targetTrack,

    @Schema(
        description = "A: priority fit; B: possible fit; C: do not apply",
        example = "A"
    )
    @NotNull(message = "Classification is required")
    JobPostingClassification classification,

    @Schema(description = "Known blocker or clarification; required for classification C")
    @Size(max = 1000, message = "Review note must not exceed 1000 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "Review note must not be blank")
    String reviewNote,

    @Schema(
        description = "Optional plain-text copy of the job advert when retention is permitted",
        maxLength = 50_000
    )
    @Size(
        max = 50_000,
        message = "Description snapshot must not exceed 50,000 characters"
    )
    @Pattern(
        regexp = "(?s).*\\S.*",
        message = "Description snapshot must not be blank"
    )
    String descriptionSnapshot
) implements JobPostingRequest {

    public UpdateJobPostingCommand toCommand() {
        return new UpdateJobPostingCommand(
            companyName,
            roleTitle,
            source,
            sourceUrl,
            externalId,
            location,
            workMode,
            foundOn,
            targetTrack,
            classification,
            reviewNote,
            descriptionSnapshot
        );
    }
}
