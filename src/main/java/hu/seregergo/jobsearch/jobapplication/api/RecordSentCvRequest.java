package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.RecordSentCvCommand;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvFileValidator;
import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Schema(description = "A PDF CV that was sent with an existing application")
public record RecordSentCvRequest(
    @Schema(example = "2026-08-01")
    @NotNull(message = "Sent date is required")
    @PastOrPresent(message = "Sent date must not be in the future")
    LocalDate sentOn,

    @Schema(example = "EN")
    @NotNull(message = "CV language is required")
    CvLanguage cvLanguage,

    @Schema(description = "PDF CV, up to 5 MiB", type = "string", format = "binary")
    @NotNull(message = "PDF CV is required")
    MultipartFile cv
) {

    public RecordSentCvCommand toCommand(SubmittedCvFileValidator validator) {
        return new RecordSentCvCommand(sentOn, cvLanguage, validator.validate(cv));
    }
}
