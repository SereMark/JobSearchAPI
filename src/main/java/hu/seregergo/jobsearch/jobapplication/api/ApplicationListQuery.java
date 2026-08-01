package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.api.validation.ValidApplicationListQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@ValidApplicationListQuery
@Schema(description = "Optional application list filters")
public record ApplicationListQuery(
    @Schema(description = "True for open work, false for closed records")
    Boolean active,

    @Schema(
        description = "Inclusive due-date cutoff; available only with active=true",
        example = "2026-08-08"
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    LocalDate dueOnOrBefore
) {
}
