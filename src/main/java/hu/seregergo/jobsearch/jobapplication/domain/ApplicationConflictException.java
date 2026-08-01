package hu.seregergo.jobsearch.jobapplication.domain;

public class ApplicationConflictException extends RuntimeException {

    private final Reason reason;

    private ApplicationConflictException(Reason reason, String message) {
        this(reason, message, null);
    }

    private ApplicationConflictException(
        Reason reason,
        String message,
        Throwable cause
    ) {
        super(message, cause);
        this.reason = reason;
    }

    public static ApplicationConflictException alreadyExists() {
        return new ApplicationConflictException(
            Reason.ALREADY_EXISTS,
            "This job posting already has an application"
        );
    }

    public static ApplicationConflictException alreadyExists(Throwable cause) {
        return new ApplicationConflictException(
            Reason.ALREADY_EXISTS,
            "This job posting already has an application",
            cause
        );
    }

    public static ApplicationConflictException ineligibleJobPosting() {
        return new ApplicationConflictException(
            Reason.INELIGIBLE_JOB_POSTING,
            "An application can only be created for an A or B job posting"
        );
    }

    public static ApplicationConflictException invalidTransition(String message) {
        return new ApplicationConflictException(Reason.INVALID_TRANSITION, message);
    }

    public static ApplicationConflictException idempotencyConflict(String message) {
        return new ApplicationConflictException(Reason.IDEMPOTENCY_CONFLICT, message);
    }

    public Reason getReason() {
        return reason;
    }

    public enum Reason {
        ALREADY_EXISTS,
        INELIGIBLE_JOB_POSTING,
        INVALID_TRANSITION,
        IDEMPOTENCY_CONFLICT
    }
}
