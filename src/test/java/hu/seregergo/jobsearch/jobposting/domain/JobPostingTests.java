package hu.seregergo.jobsearch.jobposting.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
            JobPostingClassification.A,
            null,
            CREATED_AT
        );

        assertAll(
            () -> assertEquals("Example Technologies Kft.", jobPosting.getCompanyName()),
            () -> assertEquals("Java Backend Developer", jobPosting.getRoleTitle()),
            () -> assertEquals("Company careers", jobPosting.getSource()),
            () -> assertEquals(
                "https://careers.example.com/jobs/123",
                jobPosting.getSourceUrl()
            ),
            () -> assertEquals("Budapest", jobPosting.getLocation()),
            () -> assertEquals(
                CREATED_AT.truncatedTo(ChronoUnit.MICROS),
                jobPosting.getCreatedAt()
            )
        );
    }

    @Test
    void rejectsInvalidBusinessStateWhenApiValidationIsBypassed() {
        assertAll(
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(null, null, JobPostingClassification.A, null)
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(
                    "https://example.com/jobs/123",
                    null,
                    JobPostingClassification.C,
                    null
                )
            ),
            () -> assertThrows(
                IllegalArgumentException.class,
                () -> createWith(
                    "ftp://example.com/jobs/123",
                    null,
                    JobPostingClassification.A,
                    null
                )
            )
        );
    }

    private JobPosting createWith(
        String sourceUrl,
        String externalId,
        JobPostingClassification classification,
        String reviewNote
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
            classification,
            reviewNote,
            CREATED_AT
        );
    }
}
