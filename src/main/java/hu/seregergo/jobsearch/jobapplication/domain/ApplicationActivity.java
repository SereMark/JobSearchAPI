package hu.seregergo.jobsearch.jobapplication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "application_activities")
public class ApplicationActivity {

    private static final int SUMMARY_MAX_LENGTH = 500;
    private static final int DETAILS_MAX_LENGTH = 5_000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private JobApplication application;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 32)
    private ApplicationActivityType type;

    @Column(nullable = false, length = SUMMARY_MAX_LENGTH)
    private String summary;

    @Column(length = DETAILS_MAX_LENGTH)
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApplicationActivity() {
    }

    private ApplicationActivity(
        JobApplication application,
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details,
        Instant createdAt
    ) {
        this.application = Objects.requireNonNull(
            application,
            "application must not be null"
        );
        Instant timestamp = normalizeTimestamp(createdAt, "createdAt");
        apply(validateFields(occurredAt, type, summary, details, timestamp));
        this.createdAt = timestamp;
        this.updatedAt = timestamp;
    }

    public static ApplicationActivity create(
        JobApplication application,
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details,
        Instant createdAt
    ) {
        return new ApplicationActivity(
            application,
            occurredAt,
            type,
            summary,
            details,
            createdAt
        );
    }

    public void update(
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details,
        Instant updatedAt
    ) {
        Instant timestamp = normalizeTimestamp(updatedAt, "updatedAt");
        if (timestamp.isBefore(createdAt)) {
            throw new IllegalArgumentException(
                "updatedAt must not be before createdAt"
            );
        }
        ActivityFields fields = validateFields(
            occurredAt,
            type,
            summary,
            details,
            timestamp
        );

        apply(fields);
        this.updatedAt = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public JobApplication getApplication() {
        return application;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public ApplicationActivityType getType() {
        return type;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetails() {
        return details;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void apply(ActivityFields fields) {
        this.occurredAt = fields.occurredAt();
        this.type = fields.type();
        this.summary = fields.summary();
        this.details = fields.details();
    }

    private static ActivityFields validateFields(
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details,
        Instant upperTimeBound
    ) {
        Instant normalizedOccurredAt = normalizeTimestamp(
            occurredAt,
            "occurredAt"
        );
        if (normalizedOccurredAt.isAfter(upperTimeBound)) {
            throw new IllegalArgumentException(
                "occurredAt must not be in the future"
            );
        }

        return new ActivityFields(
            normalizedOccurredAt,
            Objects.requireNonNull(type, "type must not be null"),
            requireText(summary, "summary", SUMMARY_MAX_LENGTH),
            optionalText(details, "details", DETAILS_MAX_LENGTH)
        );
    }

    private static String requireText(
        String value,
        String fieldName,
        int maxLength
    ) {
        String normalized = normalizeText(value, fieldName);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                fieldName + " must not exceed " + maxLength + " characters"
            );
        }
        return normalized;
    }

    private static String optionalText(
        String value,
        String fieldName,
        int maxLength
    ) {
        if (value == null) {
            return null;
        }
        return requireText(value, fieldName, maxLength);
    }

    private static String normalizeText(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
    }

    private static Instant normalizeTimestamp(Instant value, String fieldName) {
        return Objects.requireNonNull(
            value,
            fieldName + " must not be null"
        ).truncatedTo(ChronoUnit.MICROS);
    }

    private record ActivityFields(
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details
    ) {
    }
}
