package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.api.validation.ValidDuplicateJobPostingQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

@ValidDuplicateJobPostingQuery
@Schema(description = "Source reference used to find possible duplicate postings")
public record DuplicateJobPostingQuery(
    @Schema(example = "https://careers.example.com/jobs/123")
    @Size(max = 2048, message = "Source URL must not exceed 2048 characters")
    @URL(
        regexp = "(?i)^https?://.*$",
        message = "Source URL must be a valid HTTP or HTTPS URL"
    )
    @Pattern(regexp = "(?s).*\\S.*", message = "Source URL must not be blank")
    String sourceUrl,

    @Schema(example = "JOB-123")
    @Size(max = 200, message = "External ID must not exceed 200 characters")
    @Pattern(regexp = "(?s).*\\S.*", message = "External ID must not be blank")
    String externalId,

    @Schema(description = "Posting to omit when checking a proposed update")
    UUID excludeId
) {
}
