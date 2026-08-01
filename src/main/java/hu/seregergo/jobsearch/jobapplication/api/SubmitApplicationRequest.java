package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.SubmitApplicationCommand;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvFileValidator;
import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;
import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Schema(description = "Submission details and the optional PDF CV that was sent")
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
    LocalDate dueOn,

    @Schema(description = "Required exactly when cv is included", example = "EN")
    CvLanguage cvLanguage,

    @Schema(description = "Optional PDF CV, up to 5 MiB", type = "string", format = "binary")
    MultipartFile cv
) {

    @AssertTrue(message = "CV language is required exactly when a CV is included")
    @Schema(hidden = true)
    public boolean isCvLanguageConsistent() {
        return (cvLanguage == null) == (cv == null);
    }

    public SubmitApplicationCommand toCommand(SubmittedCvFileValidator validator) {
        PdfDocument document = cv == null ? null : validator.validate(cv);
        return new SubmitApplicationCommand(
            submittedOn,
            nextAction,
            dueOn,
            cvLanguage,
            document
        );
    }
}
