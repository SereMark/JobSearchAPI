package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;
import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Objects;

public record SubmitApplicationCommand(
    LocalDate submittedOn,
    String nextAction,
    LocalDate dueOn,
    CvLanguage cvLanguage,
    PdfDocument cv
) {

    public SubmitApplicationCommand {
        Objects.requireNonNull(submittedOn, "submittedOn must not be null");
        nextAction = Normalizer.normalize(
            Objects.requireNonNull(nextAction, "nextAction must not be null").strip(),
            Normalizer.Form.NFC
        );
        Objects.requireNonNull(dueOn, "dueOn must not be null");
        if ((cvLanguage == null) != (cv == null)) {
            throw new IllegalArgumentException(
                "cvLanguage must be provided exactly when a CV is provided"
            );
        }
    }
}
