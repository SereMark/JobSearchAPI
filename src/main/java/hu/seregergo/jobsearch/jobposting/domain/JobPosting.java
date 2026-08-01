package hu.seregergo.jobsearch.jobposting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "job_postings")
public class JobPosting {

    private static final int COMPANY_NAME_MAX_LENGTH = 200;
    private static final int ROLE_TITLE_MAX_LENGTH = 200;
    private static final int SOURCE_MAX_LENGTH = 100;
    private static final int SOURCE_URL_MAX_LENGTH = 2048;
    private static final int EXTERNAL_ID_MAX_LENGTH = 200;
    private static final int LOCATION_MAX_LENGTH = 200;
    private static final int REVIEW_NOTE_MAX_LENGTH = 1000;
    private static final int DESCRIPTION_SNAPSHOT_MAX_LENGTH = 50_000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "company_name", nullable = false, length = COMPANY_NAME_MAX_LENGTH)
    private String companyName;

    @Column(name = "role_title", nullable = false, length = ROLE_TITLE_MAX_LENGTH)
    private String roleTitle;

    @Column(nullable = false, length = SOURCE_MAX_LENGTH)
    private String source;

    @Column(name = "source_url", length = SOURCE_URL_MAX_LENGTH)
    private String sourceUrl;

    @Column(name = "external_id", length = EXTERNAL_ID_MAX_LENGTH)
    private String externalId;

    @Column(length = LOCATION_MAX_LENGTH)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 20)
    private WorkMode workMode;

    @Column(name = "found_on", nullable = false)
    private LocalDate foundOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_track", nullable = false, length = 10)
    private TargetTrack targetTrack;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private JobPostingClassification classification;

    @Column(name = "review_note", length = REVIEW_NOTE_MAX_LENGTH)
    private String reviewNote;

    @Column(name = "description_snapshot", length = DESCRIPTION_SNAPSHOT_MAX_LENGTH)
    private String descriptionSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobPosting() {
    }

    private JobPosting(
        String companyName,
        String roleTitle,
        String source,
        String sourceUrl,
        String externalId,
        String location,
        WorkMode workMode,
        LocalDate foundOn,
        TargetTrack targetTrack,
        JobPostingClassification classification,
        String reviewNote,
        String descriptionSnapshot,
        Instant createdAt
    ) {
        replaceBusinessFields(
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

        Instant timestamp = Objects.requireNonNull(
            createdAt,
            "createdAt must not be null"
        ).truncatedTo(ChronoUnit.MICROS);
        this.createdAt = timestamp;
        this.updatedAt = timestamp;
    }

    public static JobPosting create(
        String companyName,
        String roleTitle,
        String source,
        String sourceUrl,
        String externalId,
        String location,
        WorkMode workMode,
        LocalDate foundOn,
        TargetTrack targetTrack,
        JobPostingClassification classification,
        String reviewNote,
        String descriptionSnapshot,
        Instant createdAt
    ) {
        return new JobPosting(
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
            descriptionSnapshot,
            createdAt
        );
    }

    public void update(
        String companyName,
        String roleTitle,
        String source,
        String sourceUrl,
        String externalId,
        String location,
        WorkMode workMode,
        LocalDate foundOn,
        TargetTrack targetTrack,
        JobPostingClassification classification,
        String reviewNote,
        String descriptionSnapshot,
        Instant updatedAt
    ) {
        Instant normalizedUpdatedAt = Objects.requireNonNull(
            updatedAt,
            "updatedAt must not be null"
        ).truncatedTo(ChronoUnit.MICROS);
        if (normalizedUpdatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt must not be before createdAt");
        }

        replaceBusinessFields(
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
        this.updatedAt = normalizedUpdatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public String getSource() {
        return source;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getLocation() {
        return location;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public LocalDate getFoundOn() {
        return foundOn;
    }

    public TargetTrack getTargetTrack() {
        return targetTrack;
    }

    public JobPostingClassification getClassification() {
        return classification;
    }

    public String getReviewNote() {
        return reviewNote;
    }

    public String getDescriptionSnapshot() {
        return descriptionSnapshot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void replaceBusinessFields(
        String companyName,
        String roleTitle,
        String source,
        String sourceUrl,
        String externalId,
        String location,
        WorkMode workMode,
        LocalDate foundOn,
        TargetTrack targetTrack,
        JobPostingClassification classification,
        String reviewNote,
        String descriptionSnapshot
    ) {
        String normalizedCompanyName = requireText(
            companyName,
            "companyName",
            COMPANY_NAME_MAX_LENGTH
        );
        String normalizedRoleTitle = requireText(
            roleTitle,
            "roleTitle",
            ROLE_TITLE_MAX_LENGTH
        );
        String normalizedSource = requireText(source, "source", SOURCE_MAX_LENGTH);
        String normalizedSourceUrl = requireOptionalHttpUrl(sourceUrl);
        String normalizedExternalId = requireOptionalText(
            externalId,
            "externalId",
            EXTERNAL_ID_MAX_LENGTH
        );
        String normalizedLocation = requireOptionalText(
            location,
            "location",
            LOCATION_MAX_LENGTH
        );
        WorkMode normalizedWorkMode = Objects.requireNonNull(
            workMode,
            "workMode must not be null"
        );
        LocalDate normalizedFoundOn = Objects.requireNonNull(
            foundOn,
            "foundOn must not be null"
        );
        TargetTrack normalizedTargetTrack = Objects.requireNonNull(
            targetTrack,
            "targetTrack must not be null"
        );
        JobPostingClassification normalizedClassification = Objects.requireNonNull(
            classification,
            "classification must not be null"
        );
        String normalizedReviewNote = requireOptionalText(
            reviewNote,
            "reviewNote",
            REVIEW_NOTE_MAX_LENGTH
        );
        String normalizedDescriptionSnapshot = requireOptionalText(
            descriptionSnapshot,
            "descriptionSnapshot",
            DESCRIPTION_SNAPSHOT_MAX_LENGTH
        );

        requireSourceReference(normalizedSourceUrl, normalizedExternalId);
        requireReviewNoteForSkippedPosting(
            normalizedClassification,
            normalizedReviewNote
        );

        this.companyName = normalizedCompanyName;
        this.roleTitle = normalizedRoleTitle;
        this.source = normalizedSource;
        this.sourceUrl = normalizedSourceUrl;
        this.externalId = normalizedExternalId;
        this.location = normalizedLocation;
        this.workMode = normalizedWorkMode;
        this.foundOn = normalizedFoundOn;
        this.targetTrack = normalizedTargetTrack;
        this.classification = normalizedClassification;
        this.reviewNote = normalizedReviewNote;
        this.descriptionSnapshot = normalizedDescriptionSnapshot;
    }

    private static void requireSourceReference(String sourceUrl, String externalId) {
        if (sourceUrl == null && externalId == null) {
            throw new IllegalArgumentException("sourceUrl or externalId is required");
        }
    }

    private static void requireReviewNoteForSkippedPosting(
        JobPostingClassification classification,
        String reviewNote
    ) {
        if (classification == JobPostingClassification.C && reviewNote == null) {
            throw new IllegalArgumentException("reviewNote is required for C classification");
        }
    }

    private static String requireOptionalHttpUrl(String value) {
        if (value == null) {
            return null;
        }

        String url = requireText(value, "sourceUrl", SOURCE_URL_MAX_LENGTH);

        final URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                "sourceUrl must be a valid HTTP or HTTPS URL",
                exception
            );
        }

        String scheme = uri.getScheme();
        if (scheme == null
            || uri.getHost() == null
            || (!scheme.equalsIgnoreCase("http")
                && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException(
                "sourceUrl must be a valid HTTP or HTTPS URL"
            );
        }

        return url;
    }

    private static String requireText(String value, String fieldName, int maxLength) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        String normalizedValue = value.strip();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalizedValue.length() > maxLength) {
            throw new IllegalArgumentException(
                fieldName + " must not exceed " + maxLength + " characters"
            );
        }
        return normalizedValue;
    }

    private static String requireOptionalText(
        String value,
        String fieldName,
        int maxLength
    ) {
        if (value == null) {
            return null;
        }
        return requireText(value, fieldName, maxLength);
    }
}
