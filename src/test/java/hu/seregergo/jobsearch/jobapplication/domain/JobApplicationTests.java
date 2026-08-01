package hu.seregergo.jobsearch.jobapplication.domain;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobApplicationTests {

    private static final Instant CREATED_AT =
        Instant.parse("2026-08-01T08:30:00.123456789Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 1);

    @Test
    void createsActivePreparingApplicationAndNormalizesText() {
        JobApplication application = JobApplication.create(
            jobPosting(),
            "  Tailor the CV  ",
            LocalDate.of(2026, 8, 3),
            "  Emphasize recent Spring work  ",
            CREATED_AT
        );

        assertAll(
            () -> assertEquals(ApplicationStage.PREPARING, application.getStage()),
            () -> assertNull(application.getSubmittedOn()),
            () -> assertEquals("Tailor the CV", application.getNextAction()),
            () -> assertEquals(
                "Emphasize recent Spring work",
                application.getNote()
            ),
            () -> assertTrue(application.isActive()),
            () -> assertEquals(
                CREATED_AT.truncatedTo(ChronoUnit.MICROS),
                application.getCreatedAt()
            ),
            () -> assertEquals(application.getCreatedAt(), application.getUpdatedAt())
        );
    }

    @Test
    void submitsPreparedApplicationAndPreservesItsNote() {
        JobApplication application = application();
        Instant submittedAt = Instant.parse("2026-08-01T09:00:00Z");

        application.submit(
            TODAY,
            "Check for a response",
            LocalDate.of(2026, 8, 8),
            TODAY,
            submittedAt
        );

        assertAll(
            () -> assertEquals(ApplicationStage.SUBMITTED, application.getStage()),
            () -> assertEquals(TODAY, application.getSubmittedOn()),
            () -> assertEquals("Check for a response", application.getNextAction()),
            () -> assertEquals("Initial note", application.getNote()),
            () -> assertTrue(application.isActive()),
            () -> assertEquals(submittedAt, application.getUpdatedAt())
        );
    }

    @Test
    void allowsNonLinearProgressClosureAndReopeningAfterSubmission() {
        JobApplication application = submittedApplication();

        application.updateWorkflow(
            ApplicationStage.FINAL,
            "Team interview",
            "Prepare architecture examples",
            LocalDate.of(2026, 8, 10),
            null,
            "Skipped directly to the final round",
            Instant.parse("2026-08-02T09:00:00Z")
        );
        application.updateWorkflow(
            ApplicationStage.FINAL,
            null,
            null,
            null,
            ApplicationOutcome.REJECTED,
            "Role required more production experience",
            Instant.parse("2026-08-03T09:00:00Z")
        );

        assertAll(
            () -> assertFalse(application.isActive()),
            () -> assertNull(application.getNextAction()),
            () -> assertNull(application.getDueOn()),
            () -> assertEquals(ApplicationOutcome.REJECTED, application.getOutcome())
        );

        application.updateWorkflow(
            ApplicationStage.RECRUITER_SCREEN,
            null,
            "Confirm the reopened interview slot",
            LocalDate.of(2026, 8, 6),
            null,
            "Recruiter reopened the process",
            Instant.parse("2026-08-04T09:00:00Z")
        );

        assertAll(
            () -> assertTrue(application.isActive()),
            () -> assertEquals(TODAY, application.getSubmittedOn()),
            () -> assertEquals(
                ApplicationStage.RECRUITER_SCREEN,
                application.getStage()
            ),
            () -> assertNull(application.getOutcome())
        );
    }

    @Test
    void requiresSubmitEndpointForInitialSubmission() {
        JobApplication application = application();

        ApplicationConflictException exception = assertThrows(
            ApplicationConflictException.class,
            () -> application.updateWorkflow(
                ApplicationStage.SUBMITTED,
                null,
                "Check for a response",
                LocalDate.of(2026, 8, 8),
                null,
                null,
                Instant.parse("2026-08-01T09:00:00Z")
            )
        );

        assertAll(
            () -> assertEquals(
                ApplicationConflictException.Reason.INVALID_TRANSITION,
                exception.getReason()
            ),
            () -> assertEquals(ApplicationStage.PREPARING, application.getStage()),
            () -> assertNull(application.getSubmittedOn())
        );
    }

    @Test
    void closedPreparingApplicationMustBeReopenedBeforeSubmission() {
        JobApplication application = application();
        application.updateWorkflow(
            ApplicationStage.PREPARING,
            null,
            null,
            null,
            ApplicationOutcome.WITHDRAWN,
            "Decided not to apply",
            Instant.parse("2026-08-01T09:00:00Z")
        );

        assertThrows(
            ApplicationConflictException.class,
            () -> application.submit(
                TODAY,
                "Check for a response",
                LocalDate.of(2026, 8, 8),
                TODAY,
                Instant.parse("2026-08-01T10:00:00Z")
            )
        );

        application.updateWorkflow(
            ApplicationStage.PREPARING,
            null,
            "Finish the cover letter",
            LocalDate.of(2026, 8, 2),
            null,
            "Decision reconsidered",
            Instant.parse("2026-08-01T11:00:00Z")
        );
        application.submit(
            TODAY,
            "Check for a response",
            LocalDate.of(2026, 8, 8),
            TODAY,
            Instant.parse("2026-08-01T12:00:00Z")
        );

        assertEquals(ApplicationStage.SUBMITTED, application.getStage());
    }

    @Test
    void signedApplicationIsTerminal() {
        JobApplication application = submittedApplication();
        application.updateWorkflow(
            ApplicationStage.OFFER,
            null,
            null,
            null,
            ApplicationOutcome.SIGNED,
            "Contract signed",
            Instant.parse("2026-08-02T09:00:00Z")
        );

        ApplicationConflictException exception = assertThrows(
            ApplicationConflictException.class,
            () -> application.updateWorkflow(
                ApplicationStage.OFFER,
                null,
                "Undo signing",
                LocalDate.of(2026, 8, 5),
                null,
                null,
                Instant.parse("2026-08-03T09:00:00Z")
            )
        );

        assertAll(
            () -> assertEquals(
                ApplicationConflictException.Reason.INVALID_TRANSITION,
                exception.getReason()
            ),
            () -> assertEquals(ApplicationOutcome.SIGNED, application.getOutcome()),
            () -> assertFalse(application.isActive())
        );
    }

    @Test
    void rejectsInvalidWorkflowWithoutPartiallyChangingState() {
        JobApplication application = submittedApplication();

        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> application.updateWorkflow(
                    ApplicationStage.TECHNICAL_INTERVIEW,
                    null,
                    null,
                    null,
                    null,
                    "Changed note",
                    Instant.parse("2026-08-02T09:00:00Z")
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> application.updateWorkflow(
                    ApplicationStage.FINAL,
                    null,
                    null,
                    null,
                    ApplicationOutcome.SIGNED,
                    null,
                    Instant.parse("2026-08-02T09:00:00Z")
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> application().submit(
                    TODAY.plusDays(1),
                    "Check for a response",
                    LocalDate.of(2026, 8, 8),
                    TODAY,
                    Instant.parse("2026-08-02T09:00:00Z")
                )
            )
        );

        assertAll(
            () -> assertEquals(ApplicationStage.SUBMITTED, application.getStage()),
            () -> assertEquals("Check for a response", application.getNextAction()),
            () -> assertEquals("Initial note", application.getNote())
        );
    }

    private JobApplication application() {
        return JobApplication.create(
            jobPosting(),
            "Tailor the CV",
            LocalDate.of(2026, 8, 3),
            "Initial note",
            CREATED_AT
        );
    }

    private JobApplication submittedApplication() {
        JobApplication application = application();
        application.submit(
            TODAY,
            "Check for a response",
            LocalDate.of(2026, 8, 8),
            TODAY,
            Instant.parse("2026-08-01T09:00:00Z")
        );
        return application;
    }

    private JobPosting jobPosting() {
        return JobPosting.create(
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            "https://careers.example.com/jobs/123",
            null,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2026, 7, 30),
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            null,
            CREATED_AT
        );
    }
}
