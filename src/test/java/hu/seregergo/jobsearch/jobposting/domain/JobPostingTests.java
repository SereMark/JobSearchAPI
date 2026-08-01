package hu.seregergo.jobsearch.jobposting.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JobPostingTests {

    private static final Instant CREATED_AT = Instant.parse("2026-07-30T12:30:00.123456789Z");
    private static final LocalDate FOUND_ON = LocalDate.of(2026, 7, 30);

    @Test
    void createsValidJobPostingAndNormalizesText() {
        JobPosting jobPosting = JobPosting.create(
            "  Example Technologies Kft.  ",
            "  Java Backend Developer  ",
            "  Company careers  ",
            "  https://careers.example.com/jobs/123  ",
            null,
            "  Budapest  ",
            WorkMode.HYBRID,
            FOUND_ON,
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            "  Build backend services with Java.  ",
            CREATED_AT
        );

        Instant normalizedTimestamp = CREATED_AT.truncatedTo(ChronoUnit.MICROS);
        assertAll(
            () -> assertEquals("Example Technologies Kft.", jobPosting.getCompanyName()),
            () -> assertEquals("Java Backend Developer", jobPosting.getRoleTitle()),
            () -> assertEquals("Company careers", jobPosting.getSource()),
            () -> assertEquals(
                "https://careers.example.com/jobs/123",
                jobPosting.getSourceUrl()
            ),
            () -> assertEquals("Budapest", jobPosting.getLocation()),
            () -> assertEquals(TargetTrack.JAVA, jobPosting.getTargetTrack()),
            () -> assertEquals(
                "Build backend services with Java.",
                jobPosting.getDescriptionSnapshot()
            ),
            () -> assertEquals(normalizedTimestamp, jobPosting.getCreatedAt()),
            () -> assertEquals(normalizedTimestamp, jobPosting.getUpdatedAt())
        );
    }

    @Test
    void replacesEditableBusinessFieldsAndPreservesCreationTime() {
        JobPosting jobPosting = validJobPosting();
        Instant updatedAt = Instant.parse("2026-08-01T10:15:30.123456789Z");

        jobPosting.update(
            "Updated Company",
            ".NET Backend Developer",
            "Recruiter",
            null,
            "DOTNET-456",
            "Budapest",
            WorkMode.REMOTE,
            FOUND_ON,
            TargetTrack.DOTNET,
            JobPostingClassification.B,
            "One clarification remains",
            null,
            updatedAt
        );

        assertAll(
            () -> assertEquals("Updated Company", jobPosting.getCompanyName()),
            () -> assertEquals(".NET Backend Developer", jobPosting.getRoleTitle()),
            () -> assertEquals(TargetTrack.DOTNET, jobPosting.getTargetTrack()),
            () -> assertEquals("DOTNET-456", jobPosting.getExternalId()),
            () -> assertNull(jobPosting.getSourceUrl()),
            () -> assertNull(jobPosting.getDescriptionSnapshot()),
            () -> assertEquals(
                CREATED_AT.truncatedTo(ChronoUnit.MICROS),
                jobPosting.getCreatedAt()
            ),
            () -> assertEquals(
                updatedAt.truncatedTo(ChronoUnit.MICROS),
                jobPosting.getUpdatedAt()
            )
        );
    }

    @Test
    void leavesExistingStateUntouchedWhenUpdateIsInvalid() {
        JobPosting jobPosting = validJobPosting();

        assertThrows(
            IllegalArgumentException.class,
            () -> jobPosting.update(
                "Changed Company",
                "Changed role",
                "Company careers",
                "https://example.com/jobs/changed",
                null,
                "Budapest",
                WorkMode.HYBRID,
                FOUND_ON,
                TargetTrack.JAVA,
                JobPostingClassification.C,
                null,
                null,
                Instant.parse("2026-08-01T10:15:30Z")
            )
        );

        assertAll(
            () -> assertEquals("Example Technologies Kft.", jobPosting.getCompanyName()),
            () -> assertEquals("Java Backend Developer", jobPosting.getRoleTitle()),
            () -> assertEquals(
                CREATED_AT.truncatedTo(ChronoUnit.MICROS),
                jobPosting.getUpdatedAt()
            )
        );
    }

    @Test
    void rejectsInvalidBusinessStateWhenApiValidationIsBypassed() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(null, null, TargetTrack.JAVA, JobPostingClassification.A, null, null)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(
                    "https://example.com/jobs/123",
                    null,
                    TargetTrack.JAVA,
                    JobPostingClassification.C,
                    null,
                    null
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(
                    "ftp://example.com/jobs/123",
                    null,
                    TargetTrack.JAVA,
                    JobPostingClassification.A,
                    null,
                    null
                )
            ),
            () -> assertThrows(
                NullPointerException.class,
                () -> createWith(
                    "https://example.com/jobs/123",
                    null,
                    null,
                    JobPostingClassification.A,
                    null,
                    null
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(
                    "https://example.com/jobs/123",
                    null,
                    TargetTrack.JAVA,
                    JobPostingClassification.A,
                    null,
                    "   "
                )
            )
        );
    }

    private JobPosting validJobPosting() {
        return createWith(
            "https://careers.example.com/jobs/123",
            null,
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            "Build backend services with Java."
        );
    }

    private JobPosting createWith(
        String sourceUrl,
        String externalId,
        TargetTrack targetTrack,
        JobPostingClassification classification,
        String reviewNote,
        String descriptionSnapshot
    ) {
        return JobPosting.create(
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            sourceUrl,
            externalId,
            "Budapest",
            WorkMode.HYBRID,
            FOUND_ON,
            targetTrack,
            classification,
            reviewNote,
            descriptionSnapshot,
            CREATED_AT
        );
    }
}
