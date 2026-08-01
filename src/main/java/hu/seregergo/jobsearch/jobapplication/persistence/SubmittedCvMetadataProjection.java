package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public interface SubmittedCvMetadataProjection {

    UUID getApplicationId();

    LocalDate getSentOn();

    CvLanguage getLanguage();

    String getOriginalFileName();

    long getSizeBytes();

    String getSha256();

    Instant getRecordedAt();
}
