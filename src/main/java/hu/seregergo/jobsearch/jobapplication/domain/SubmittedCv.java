package hu.seregergo.jobsearch.jobapplication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "submitted_cvs")
public class SubmittedCv {

    @Id
    @Column(name = "application_id", nullable = false, updatable = false)
    private UUID applicationId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private JobApplication application;

    @Column(name = "sent_on", nullable = false)
    private LocalDate sentOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private CvLanguage language;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] bytes;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    protected SubmittedCv() {
    }

    private SubmittedCv(
        JobApplication application,
        LocalDate sentOn,
        CvLanguage language,
        PdfDocument document,
        LocalDate today,
        Instant recordedAt
    ) {
        this.application = Objects.requireNonNull(
            application,
            "application must not be null"
        );
        this.sentOn = Objects.requireNonNull(sentOn, "sentOn must not be null");
        this.language = Objects.requireNonNull(language, "language must not be null");
        PdfDocument normalizedDocument = Objects.requireNonNull(
            document,
            "document must not be null"
        );

        LocalDate submittedOn = application.getSubmittedOn();
        if (submittedOn == null || this.sentOn.isBefore(submittedOn)) {
            throw new IllegalArgumentException(
                "sentOn must not be before the application submission date"
            );
        }
        if (this.sentOn.isAfter(Objects.requireNonNull(today, "today must not be null"))) {
            throw new IllegalArgumentException("sentOn must not be in the future");
        }

        this.originalFileName = normalizedDocument.originalFileName();
        this.sizeBytes = normalizedDocument.sizeBytes();
        this.sha256 = normalizedDocument.sha256();
        this.bytes = normalizedDocument.bytes();
        this.recordedAt = Objects.requireNonNull(
            recordedAt,
            "recordedAt must not be null"
        ).truncatedTo(ChronoUnit.MICROS);
    }

    public static SubmittedCv create(
        JobApplication application,
        LocalDate sentOn,
        CvLanguage language,
        PdfDocument document,
        LocalDate today,
        Instant recordedAt
    ) {
        return new SubmittedCv(
            application,
            sentOn,
            language,
            document,
            today,
            recordedAt
        );
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public LocalDate getSentOn() {
        return sentOn;
    }

    public CvLanguage getLanguage() {
        return language;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getSha256() {
        return sha256;
    }

    public byte[] getBytes() {
        return bytes.clone();
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
