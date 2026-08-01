package hu.seregergo.jobsearch.jobapplication.domain;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class JobApplication {

    private static final int STAGE_LABEL_MAX_LENGTH = 100;
    private static final int NEXT_ACTION_MAX_LENGTH = 500;
    private static final int NOTE_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_posting_id", nullable = false, updatable = false)
    private JobPosting jobPosting;

    @Column(name = "submitted_on")
    private LocalDate submittedOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStage stage;

    @Column(name = "stage_label", length = STAGE_LABEL_MAX_LENGTH)
    private String stageLabel;

    @Column(name = "next_action", length = NEXT_ACTION_MAX_LENGTH)
    private String nextAction;

    @Column(name = "due_on")
    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ApplicationOutcome outcome;

    @Column(length = NOTE_MAX_LENGTH)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobApplication() {
    }

    private JobApplication(
        JobPosting jobPosting,
        String nextAction,
        LocalDate dueOn,
        String note,
        Instant createdAt
    ) {
        this.jobPosting = Objects.requireNonNull(
            jobPosting,
            "jobPosting must not be null"
        );
        Workflow workflow = validateWorkflow(
            ApplicationStage.PREPARING,
            null,
            nextAction,
            dueOn,
            null,
            note,
            null
        );
        apply(workflow);

        Instant timestamp = normalizeTimestamp(createdAt, null);
        this.createdAt = timestamp;
        this.updatedAt = timestamp;
    }

    public static JobApplication create(
        JobPosting jobPosting,
        String nextAction,
        LocalDate dueOn,
        String note,
        Instant createdAt
    ) {
        return new JobApplication(jobPosting, nextAction, dueOn, note, createdAt);
    }

    public void updateWorkflow(
        ApplicationStage stage,
        String stageLabel,
        String nextAction,
        LocalDate dueOn,
        ApplicationOutcome outcome,
        String note,
        Instant updatedAt
    ) {
        requireMutable();
        ApplicationStage requestedStage = Objects.requireNonNull(
            stage,
            "stage must not be null"
        );
        validateTransition(requestedStage);

        Workflow workflow = validateWorkflow(
            requestedStage,
            stageLabel,
            nextAction,
            dueOn,
            outcome,
            note,
            submittedOn
        );
        Instant timestamp = normalizeTimestamp(updatedAt, createdAt);

        apply(workflow);
        this.updatedAt = timestamp;
    }

    public void submit(
        LocalDate submittedOn,
        String nextAction,
        LocalDate dueOn,
        LocalDate today,
        Instant updatedAt
    ) {
        if (stage != ApplicationStage.PREPARING
            || this.submittedOn != null
            || outcome != null) {
            throw ApplicationConflictException.invalidTransition(
                "Only an active PREPARING application can be submitted"
            );
        }

        LocalDate normalizedSubmittedOn = Objects.requireNonNull(
            submittedOn,
            "submittedOn must not be null"
        );
        LocalDate currentDate = Objects.requireNonNull(today, "today must not be null");
        if (normalizedSubmittedOn.isAfter(currentDate)) {
            throw new IllegalArgumentException("submittedOn must not be in the future");
        }

        Workflow workflow = validateWorkflow(
            ApplicationStage.SUBMITTED,
            null,
            nextAction,
            dueOn,
            null,
            note,
            normalizedSubmittedOn
        );
        Instant timestamp = normalizeTimestamp(updatedAt, createdAt);

        this.submittedOn = normalizedSubmittedOn;
        apply(workflow);
        this.updatedAt = timestamp;
    }

    public UUID getId() {
        return id;
    }

    public JobPosting getJobPosting() {
        return jobPosting;
    }

    public LocalDate getSubmittedOn() {
        return submittedOn;
    }

    public ApplicationStage getStage() {
        return stage;
    }

    public String getStageLabel() {
        return stageLabel;
    }

    public String getNextAction() {
        return nextAction;
    }

    public LocalDate getDueOn() {
        return dueOn;
    }

    public ApplicationOutcome getOutcome() {
        return outcome;
    }

    public String getNote() {
        return note;
    }

    public boolean isActive() {
        return outcome == null;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void validateTransition(ApplicationStage requestedStage) {
        if (submittedOn == null && requestedStage != ApplicationStage.PREPARING) {
            if (outcome != null) {
                throw ApplicationConflictException.invalidTransition(
                    "Reopen the PREPARING application before submitting it"
                );
            }
            throw ApplicationConflictException.invalidTransition(
                "Use the submit endpoint to move a PREPARING application forward"
            );
        }
        if (submittedOn != null && requestedStage == ApplicationStage.PREPARING) {
            throw ApplicationConflictException.invalidTransition(
                "A submitted application cannot return to PREPARING"
            );
        }
    }

    private void requireMutable() {
        if (outcome == ApplicationOutcome.SIGNED) {
            throw ApplicationConflictException.invalidTransition(
                "A signed application is final and cannot be changed"
            );
        }
    }

    private void apply(Workflow workflow) {
        this.stage = workflow.stage();
        this.stageLabel = workflow.stageLabel();
        this.nextAction = workflow.nextAction();
        this.dueOn = workflow.dueOn();
        this.outcome = workflow.outcome();
        this.note = workflow.note();
    }

    private static Workflow validateWorkflow(
        ApplicationStage stage,
        String stageLabel,
        String nextAction,
        LocalDate dueOn,
        ApplicationOutcome outcome,
        String note,
        LocalDate submittedOn
    ) {
        ApplicationStage normalizedStage = Objects.requireNonNull(
            stage,
            "stage must not be null"
        );
        String normalizedStageLabel = optionalText(
            stageLabel,
            "stageLabel",
            STAGE_LABEL_MAX_LENGTH
        );
        String normalizedNextAction = optionalText(
            nextAction,
            "nextAction",
            NEXT_ACTION_MAX_LENGTH
        );
        String normalizedNote = optionalText(note, "note", NOTE_MAX_LENGTH);

        if (outcome == null && (normalizedNextAction == null || dueOn == null)) {
            throw new IllegalArgumentException(
                "An active application requires nextAction and dueOn"
            );
        }
        if (outcome != null && (normalizedNextAction != null || dueOn != null)) {
            throw new IllegalArgumentException(
                "A closed application cannot have nextAction or dueOn"
            );
        }
        if (normalizedStage == ApplicationStage.PREPARING && submittedOn != null) {
            throw new IllegalArgumentException(
                "A PREPARING application cannot have submittedOn"
            );
        }
        if (normalizedStage != ApplicationStage.PREPARING && submittedOn == null) {
            throw new IllegalArgumentException(
                "A submitted stage requires submittedOn"
            );
        }
        if ((outcome == ApplicationOutcome.SIGNED
            || outcome == ApplicationOutcome.OFFER_DECLINED)
            && normalizedStage != ApplicationStage.OFFER) {
            throw new IllegalArgumentException(
                "SIGNED and OFFER_DECLINED outcomes require the OFFER stage"
            );
        }
        if (normalizedStage == ApplicationStage.PREPARING
            && outcome != null
            && outcome != ApplicationOutcome.WITHDRAWN
            && outcome != ApplicationOutcome.ROLE_CANCELLED) {
            throw new IllegalArgumentException(
                "A PREPARING application can only close as WITHDRAWN or ROLE_CANCELLED"
            );
        }

        return new Workflow(
            normalizedStage,
            normalizedStageLabel,
            normalizedNextAction,
            dueOn,
            outcome,
            normalizedNote
        );
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

    private static String optionalText(String value, String fieldName, int maxLength) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();
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

    private record Workflow(
        ApplicationStage stage,
        String stageLabel,
        String nextAction,
        LocalDate dueOn,
        ApplicationOutcome outcome,
        String note
    ) {
    }
}
