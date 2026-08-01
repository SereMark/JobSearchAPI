package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;
import hu.seregergo.jobsearch.jobapplication.domain.SubmittedCv;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvMetadataProjection;

import java.time.Instant;
import java.time.LocalDate;

public record SubmittedCvMetadata(
    LocalDate sentOn,
    CvLanguage language,
    String originalFileName,
    long sizeBytes,
    String sha256,
    Instant recordedAt
) {

    public static SubmittedCvMetadata from(SubmittedCv cv) {
        return new SubmittedCvMetadata(
            cv.getSentOn(),
            cv.getLanguage(),
            cv.getOriginalFileName(),
            cv.getSizeBytes(),
            cv.getSha256(),
            cv.getRecordedAt()
        );
    }

    public static SubmittedCvMetadata from(SubmittedCvMetadataProjection cv) {
        return new SubmittedCvMetadata(
            cv.getSentOn(),
            cv.getLanguage(),
            cv.getOriginalFileName(),
            cv.getSizeBytes(),
            cv.getSha256(),
            cv.getRecordedAt()
        );
    }
}
