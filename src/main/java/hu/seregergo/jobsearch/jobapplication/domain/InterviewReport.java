package hu.seregergo.jobsearch.jobapplication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "interview_reports")
public class InterviewReport {

    private static final int ROUND_LABEL_MAX_LENGTH = 200;
    private static final int REPORT_MAX_LENGTH = 20_000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private JobApplication application;

    @Column(name = "interviewed_on", nullable = false)
    private LocalDate interviewedOn;

    @Column(name = "round_label", nullable = false, length = ROUND_LABEL_MAX_LENGTH)
    private String roundLabel;

    @Column(nullable = false, length = REPORT_MAX_LENGTH)
    private String report;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected InterviewReport() {
    }

    private InterviewReport(
        JobApplication application,
        LocalDate interviewedOn,
        String roundLabel,
        String report,
        LocalDate today,
        Instant createdAt
    ) {
        this.application = Objects.requireNonNull(
            application,
            "application must not be null"
        );
        apply(validateFields(interviewedOn, roundLabel, report, today));

        Instant timestamp = normalizeTimestamp(createdAt, null);
        this.createdAt = timestamp;
        this.updatedAt = timestamp;
    }

    public static InterviewReport create(
        JobApplication application,
        LocalDate interviewedOn,
        String roundLabel,
        String report,
        LocalDate today,
        Instant createdAt
    ) {
        return new InterviewReport(
            application,
            interviewedOn,
            roundLabel,
            report,
            today,
            createdAt
        );
    }

    public void update(
        LocalDate interviewedOn,
        String roundLabel,
        String report,
        LocalDate today,
        Instant updatedAt
    ) {
        InterviewFields fields = validateFields(
            interviewedOn,
            roundLabel,
            report,
            today
        );
        Instant timestamp = normalizeTimestamp(updatedAt, createdAt);

        apply(fields);
        this.updatedAt = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public JobApplication getApplication() {
        return application;
    }

    public LocalDate getInterviewedOn() {
        return interviewedOn;
    }

    public String getRoundLabel() {
        return roundLabel;
    }

    public String getReport() {
        return report;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void apply(InterviewFields fields) {
        this.interviewedOn = fields.interviewedOn();
        this.roundLabel = fields.roundLabel();
        this.report = fields.report();
    }

    private static InterviewFields validateFields(
        LocalDate interviewedOn,
        String roundLabel,
        String report,
        LocalDate today
    ) {
        LocalDate normalizedInterviewedOn = Objects.requireNonNull(
            interviewedOn,
            "interviewedOn must not be null"
        );
        if (normalizedInterviewedOn.isAfter(Objects.requireNonNull(
            today,
            "today must not be null"
        ))) {
            throw new IllegalArgumentException(
                "interviewedOn must not be in the future"
            );
        }

        return new InterviewFields(
            normalizedInterviewedOn,
            requireText(roundLabel, "roundLabel", ROUND_LABEL_MAX_LENGTH),
            requireText(report, "report", REPORT_MAX_LENGTH)
        );
    }

    private static String requireText(
        String value,
        String fieldName,
        int maxLength
    ) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }

        String normalized = Normalizer.normalize(value.strip(), Normalizer.Form.NFC);
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

    private static Instant normalizeTimestamp(Instant value, Instant lowerBound) {
        Instant timestamp = Objects.requireNonNull(
            value,
            "timestamp must not be null"
        ).truncatedTo(ChronoUnit.MICROS);
        if (lowerBound != null && timestamp.isBefore(lowerBound)) {
            throw new IllegalArgumentException(
                "updatedAt must not be before createdAt"
            );
        }
        return timestamp;
    }

    private record InterviewFields(
        LocalDate interviewedOn,
        String roundLabel,
        String report
    ) {
    }
}
