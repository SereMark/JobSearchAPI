package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;
import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;

import java.time.LocalDate;
import java.util.Objects;

public record RecordSentCvCommand(
    LocalDate sentOn,
    CvLanguage cvLanguage,
    PdfDocument cv
) {

    public RecordSentCvCommand {
        Objects.requireNonNull(sentOn, "sentOn must not be null");
        Objects.requireNonNull(cvLanguage, "cvLanguage must not be null");
        Objects.requireNonNull(cv, "cv must not be null");
    }
}
