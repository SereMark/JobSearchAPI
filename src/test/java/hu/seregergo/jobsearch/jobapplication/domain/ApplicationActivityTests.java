package hu.seregergo.jobsearch.jobapplication.domain;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationActivityTests {

    private static final Instant CREATED_AT = Instant.parse(
        "2026-08-01T12:00:00.123456789Z"
    );

    @Test
    void createsAndNormalizesAnApplicationActivity() {
        JobApplication application = application();

        ApplicationActivity activity = ApplicationActivity.create(
            application,
            Instant.parse("2026-08-01T10:30:00.987654321Z"),
            ApplicationActivityType.EMAIL,
            "  Toborzói visszajelzés  ",
            "  Megero\u030Bsi\u0301tette a technikai interju\u0301 ido\u030Bpontja\u0301t.  ",
            CREATED_AT
        );

        assertAll(
            () -> assertSame(application, activity.getApplication()),
            () -> assertEquals(
                Instant.parse("2026-08-01T10:30:00.987654Z"),
                activity.getOccurredAt()
            ),
            () -> assertEquals(ApplicationActivityType.EMAIL, activity.getType()),
            () -> assertEquals("Toborzói visszajelzés", activity.getSummary()),
            () -> assertEquals(
                "Megerősítette a technikai interjú időpontját.",
                activity.getDetails()
            ),
            () -> assertEquals(
                Instant.parse("2026-08-01T12:00:00.123456Z"),
                activity.getCreatedAt()
            ),
            () -> assertEquals(activity.getCreatedAt(), activity.getUpdatedAt())
        );
    }

    @Test
    void replacesEditableFieldsAndKeepsCreationTime() {
        ApplicationActivity activity = activity();
        Instant updatedAt = Instant.parse("2026-08-02T09:00:00.987654321Z");

        activity.update(
            Instant.parse("2026-08-02T08:30:00.123456789Z"),
            ApplicationActivityType.FOLLOW_UP,
            "Sent a follow-up",
            null,
            updatedAt
        );

        assertAll(
            () -> assertEquals(
                Instant.parse("2026-08-02T08:30:00.123456Z"),
                activity.getOccurredAt()
            ),
            () -> assertEquals(
                ApplicationActivityType.FOLLOW_UP,
                activity.getType()
            ),
            () -> assertEquals("Sent a follow-up", activity.getSummary()),
            () -> assertNull(activity.getDetails()),
            () -> assertEquals(
                Instant.parse("2026-08-01T12:00:00.123456Z"),
                activity.getCreatedAt()
            ),
            () -> assertEquals(
                Instant.parse("2026-08-02T09:00:00.987654Z"),
                activity.getUpdatedAt()
            )
        );
    }

    @Test
    void rejectsInvalidFields() {
        JobApplication application = application();

        assertAll(
            () -> assertThrows(
                NullPointerException.class,
                () -> ApplicationActivity.create(
                    application,
                    null,
                    ApplicationActivityType.EMAIL,
                    "Recruiter replied",
                    null,
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                NullPointerException.class,
                () -> ApplicationActivity.create(
                    application,
                    CREATED_AT.minusSeconds(60),
                    null,
                    "Recruiter replied",
                    null,
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> ApplicationActivity.create(
                    application,
                    CREATED_AT.minusSeconds(60),
                    ApplicationActivityType.EMAIL,
                    "   ",
                    null,
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> ApplicationActivity.create(
                    application,
                    CREATED_AT.minusSeconds(60),
                    ApplicationActivityType.EMAIL,
                    "x".repeat(501),
                    null,
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> ApplicationActivity.create(
                    application,
                    CREATED_AT.minusSeconds(60),
                    ApplicationActivityType.EMAIL,
                    "Recruiter replied",
                    "   ",
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> ApplicationActivity.create(
                    application,
                    CREATED_AT.minusSeconds(60),
                    ApplicationActivityType.EMAIL,
                    "Recruiter replied",
                    "x".repeat(5_001),
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> ApplicationActivity.create(
                    application,
                    CREATED_AT.plusSeconds(1),
                    ApplicationActivityType.EMAIL,
                    "Recruiter replied",
                    null,
                    CREATED_AT
                )
            )
        );
    }

    @Test
    void rejectsAnInvalidUpdateWithoutPartiallyChangingTheActivity() {
        ApplicationActivity activity = activity();

        assertThrows(
            IllegalArgumentException.class,
            () -> activity.update(
                CREATED_AT.minusSeconds(30),
                ApplicationActivityType.CALL,
                "Changed summary",
                "Changed details",
                CREATED_AT.minusSeconds(1)
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> activity.update(
                CREATED_AT.plusSeconds(120),
                ApplicationActivityType.CALL,
                "Changed summary",
                "Changed details",
                CREATED_AT.plusSeconds(60)
            )
        );

        assertAll(
            () -> assertEquals(
                Instant.parse("2026-08-01T11:59:00.123456Z"),
                activity.getOccurredAt()
            ),
            () -> assertEquals(ApplicationActivityType.EMAIL, activity.getType()),
            () -> assertEquals("Recruiter replied", activity.getSummary()),
            () -> assertEquals("Technical interview confirmed.", activity.getDetails()),
            () -> assertEquals(activity.getCreatedAt(), activity.getUpdatedAt())
        );
    }

    private ApplicationActivity activity() {
        return ApplicationActivity.create(
            application(),
            CREATED_AT.minusSeconds(60),
            ApplicationActivityType.EMAIL,
            "Recruiter replied",
            "Technical interview confirmed.",
            CREATED_AT
        );
    }

    private JobApplication application() {
        return JobApplication.create(
            JobPosting.create(
                "Example Technologies Kft.",
                "Java Backend Developer",
                "Company careers",
                "https://example.com/jobs/123",
                null,
                "Budapest",
                WorkMode.HYBRID,
                LocalDate.of(2026, 7, 30),
                TargetTrack.JAVA,
                JobPostingClassification.A,
                null,
                null,
                Instant.parse("2026-07-30T08:00:00Z")
            ),
            "Prepare for the interview",
            LocalDate.of(2026, 8, 3),
            null,
            Instant.parse("2026-07-30T08:00:00Z")
        );
    }
}
