package hu.seregergo.jobsearch.jobapplication.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Entity
@Table(
    name = "application_idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "application_idempotency_operation_unique",
        columnNames = {"application_id", "operation"}
    )
)
public class ApplicationIdempotencyRecord {

    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, updatable = false)
    private JobApplication application;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private IdempotencyOperation operation;

    @Column(name = "request_fingerprint", nullable = false, length = 64, updatable = false)
    private String requestFingerprint;

    @Column(name = "response_status", nullable = false, updatable = false)
    private int responseStatus;

    @Column(name = "response_body", nullable = false, updatable = false, columnDefinition = "text")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ApplicationIdempotencyRecord() {
    }

    private ApplicationIdempotencyRecord(
        UUID idempotencyKey,
        JobApplication application,
        IdempotencyOperation operation,
        String requestFingerprint,
        int responseStatus,
        String responseBody,
        Instant createdAt
    ) {
        this.idempotencyKey = Objects.requireNonNull(
            idempotencyKey,
            "idempotencyKey must not be null"
        );
        this.application = Objects.requireNonNull(
            application,
            "application must not be null"
        );
        this.operation = Objects.requireNonNull(operation, "operation must not be null");
        this.requestFingerprint = requireSha256(requestFingerprint);
        if (responseStatus < 200 || responseStatus > 299) {
            throw new IllegalArgumentException("responseStatus must be successful");
        }
        this.responseStatus = responseStatus;
        this.responseBody = requireText(responseBody, "responseBody");
        this.createdAt = Objects.requireNonNull(
            createdAt,
            "createdAt must not be null"
        ).truncatedTo(ChronoUnit.MICROS);
    }

    public static ApplicationIdempotencyRecord create(
        UUID idempotencyKey,
        JobApplication application,
        IdempotencyOperation operation,
        String requestFingerprint,
        int responseStatus,
        String responseBody,
        Instant createdAt
    ) {
        return new ApplicationIdempotencyRecord(
            idempotencyKey,
            application,
            operation,
            requestFingerprint,
            responseStatus,
            responseBody,
            createdAt
        );
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public UUID getApplicationId() {
        return application.getId();
    }

    public IdempotencyOperation getOperation() {
        return operation;
    }

    public String getRequestFingerprint() {
        return requestFingerprint;
    }

    public int getResponseStatus() {
        return responseStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireSha256(String value) {
        String fingerprint = requireText(value, "requestFingerprint");
        if (!SHA256.matcher(fingerprint).matches()) {
            throw new IllegalArgumentException(
                "requestFingerprint must be a lowercase SHA-256 value"
            );
        }
        return fingerprint;
    }

    private static String requireText(String value, String fieldName) {
        String text = Objects.requireNonNull(
            value,
            fieldName + " must not be null"
        );
        if (text.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return text;
    }
}
