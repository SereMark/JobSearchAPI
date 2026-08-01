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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InterviewReportTests {

    private static final Instant CREATED_AT = Instant.parse(
        "2026-08-01T08:00:00.123456789Z"
    );

    @Test
    void createsAndNormalizesAnInterviewReport() {
        JobApplication application = application();

        InterviewReport report = InterviewReport.create(
            application,
            LocalDate.of(2026, 8, 1),
            "  Technikai ko\u0308r  ",
            "  Jo\u0301l ment a rendszertervezési beszélgetés.  ",
            LocalDate.of(2026, 8, 1),
            CREATED_AT
        );

        assertAll(
            () -> assertSame(application, report.getApplication()),
            () -> assertEquals(LocalDate.of(2026, 8, 1), report.getInterviewedOn()),
            () -> assertEquals("Technikai kör", report.getRoundLabel()),
            () -> assertEquals(
                "Jól ment a rendszertervezési beszélgetés.",
                report.getReport()
            ),
            () -> assertEquals(
                Instant.parse("2026-08-01T08:00:00.123456Z"),
                report.getCreatedAt()
            ),
            () -> assertEquals(report.getCreatedAt(), report.getUpdatedAt())
        );
    }

    @Test
    void replacesEditableFieldsAndKeepsCreationTime() {
        InterviewReport report = report();
        Instant updatedAt = Instant.parse("2026-08-02T09:00:00.987654321Z");

        report.update(
            LocalDate.of(2026, 8, 2),
            "Hiring manager",
            "Clear discussion about ownership and expectations.",
            LocalDate.of(2026, 8, 2),
            updatedAt
        );

        assertAll(
            () -> assertEquals(LocalDate.of(2026, 8, 2), report.getInterviewedOn()),
            () -> assertEquals("Hiring manager", report.getRoundLabel()),
            () -> assertEquals(
                "Clear discussion about ownership and expectations.",
                report.getReport()
            ),
            () -> assertEquals(
                Instant.parse("2026-08-01T08:00:00.123456Z"),
                report.getCreatedAt()
            ),
            () -> assertEquals(
                Instant.parse("2026-08-02T09:00:00.987654Z"),
                report.getUpdatedAt()
            )
        );
    }

    @Test
    void rejectsInvalidFields() {
        JobApplication application = application();

        assertAll(
            () -> assertThrows(
                NullPointerException.class,
                () -> InterviewReport.create(
                    application,
                    null,
                    "Recruiter screen",
                    "Useful conversation",
                    LocalDate.of(2026, 8, 1),
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> InterviewReport.create(
                    application,
                    LocalDate.of(2026, 8, 1),
                    "   ",
                    "Useful conversation",
                    LocalDate.of(2026, 8, 1),
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> InterviewReport.create(
                    application,
                    LocalDate.of(2026, 8, 1),
                    "Recruiter screen",
                    "x".repeat(20_001),
                    LocalDate.of(2026, 8, 1),
                    CREATED_AT
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> InterviewReport.create(
                    application,
                    LocalDate.of(2026, 8, 2),
                    "Recruiter screen",
                    "Useful conversation",
                    LocalDate.of(2026, 8, 1),
                    CREATED_AT
                )
            )
        );
    }

    @Test
    void rejectsAnInvalidUpdateWithoutPartiallyChangingTheReport() {
        InterviewReport report = report();

        assertThrows(
            IllegalArgumentException.class,
            () -> report.update(
                LocalDate.of(2026, 8, 2),
                "   ",
                "Changed report",
                LocalDate.of(2026, 8, 2),
                Instant.parse("2026-08-02T09:00:00Z")
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> report.update(
                LocalDate.of(2026, 8, 2),
                "Changed round",
                "Changed report",
                LocalDate.of(2026, 8, 2),
                Instant.parse("2026-07-31T09:00:00Z")
            )
        );

        assertAll(
            () -> assertEquals(LocalDate.of(2026, 8, 1), report.getInterviewedOn()),
            () -> assertEquals("Recruiter screen", report.getRoundLabel()),
            () -> assertEquals("Positive first conversation.", report.getReport()),
            () -> assertEquals(report.getCreatedAt(), report.getUpdatedAt())
        );
    }

    private InterviewReport report() {
        return InterviewReport.create(
            application(),
            LocalDate.of(2026, 8, 1),
            "Recruiter screen",
            "Positive first conversation.",
            LocalDate.of(2026, 8, 1),
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
            LocalDate.of(2026, 8, 1),
            null,
            Instant.parse("2026-07-30T08:00:00Z")
        );
    }
}
