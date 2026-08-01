package hu.seregergo.jobsearch.jobposting.persistence;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobPostingRepositoryTests extends PostgreSqlIntegrationTest {

    @Autowired
    private JobPostingRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndLoadsCompleteJobPosting() {
        Instant createdAt = Instant.parse("2026-07-30T12:30:00Z");
        LocalDate foundOn = LocalDate.of(2026, 7, 29);
        JobPosting jobPosting = JobPosting.create(
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            "https://careers.example.com/jobs/123",
            "JOB-123",
            "Budapest",
            WorkMode.HYBRID,
            foundOn,
            TargetTrack.JAVA,
            JobPostingClassification.B,
            "One optional technology is missing",
            "Build and maintain backend services.",
            createdAt
        );

        JobPosting savedJobPosting = repository.saveAndFlush(jobPosting);
        entityManager.clear();

        JobPosting loadedJobPosting = repository.findById(savedJobPosting.getId())
            .orElseThrow();

        assertAll(
            () -> assertNotNull(loadedJobPosting.getId()),
            () -> assertEquals(
                "Example Technologies Kft.",
                loadedJobPosting.getCompanyName()
            ),
            () -> assertEquals("Java Backend Developer", loadedJobPosting.getRoleTitle()),
            () -> assertEquals("Company careers", loadedJobPosting.getSource()),
            () -> assertEquals(
                "https://careers.example.com/jobs/123",
                loadedJobPosting.getSourceUrl()
            ),
            () -> assertEquals("JOB-123", loadedJobPosting.getExternalId()),
            () -> assertEquals("Budapest", loadedJobPosting.getLocation()),
            () -> assertEquals(WorkMode.HYBRID, loadedJobPosting.getWorkMode()),
            () -> assertEquals(foundOn, loadedJobPosting.getFoundOn()),
            () -> assertEquals(TargetTrack.JAVA, loadedJobPosting.getTargetTrack()),
            () -> assertEquals(
                JobPostingClassification.B,
                loadedJobPosting.getClassification()
            ),
            () -> assertEquals(
                "One optional technology is missing",
                loadedJobPosting.getReviewNote()
            ),
            () -> assertEquals(
                "Build and maintain backend services.",
                loadedJobPosting.getDescriptionSnapshot()
            ),
            () -> assertEquals(createdAt, loadedJobPosting.getCreatedAt()),
            () -> assertEquals(createdAt, loadedJobPosting.getUpdatedAt())
        );
    }

    @Test
    void databaseRejectsMissingSourceReferenceWhenApplicationIsBypassed() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawJobPosting(null, null, "JAVA", null)
        );
    }

    @Test
    void databaseRejectsInvalidTargetTrackWhenApplicationIsBypassed() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawJobPosting(
                "https://example.com/jobs/123",
                null,
                "PYTHON",
                null
            )
        );
    }

    @Test
    void databaseRejectsBlankDescriptionSnapshotWhenApplicationIsBypassed() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawJobPosting(
                "https://example.com/jobs/123",
                null,
                "JAVA",
                "   "
            )
        );
    }

    @Test
    void databaseRejectsUpdatedAtBeforeCreatedAtWhenApplicationIsBypassed() {
        Instant createdAt = Instant.parse("2026-07-30T12:30:00Z");
        assertThrows(
            DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(
                """
                    INSERT INTO job_postings (
                        id,
                        company_name,
                        role_title,
                        source,
                        source_url,
                        work_mode,
                        found_on,
                        target_track,
                        classification,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                UUID.randomUUID(),
                "Example Technologies Kft.",
                "Java Backend Developer",
                "Company careers",
                "https://example.com/jobs/123",
                "HYBRID",
                Date.valueOf(LocalDate.of(2026, 7, 29)),
                "JAVA",
                "A",
                Timestamp.from(createdAt),
                Timestamp.from(createdAt.minusSeconds(1))
            )
        );
    }

    private void insertRawJobPosting(
        String sourceUrl,
        String externalId,
        String targetTrack,
        String descriptionSnapshot
    ) {
        Instant timestamp = Instant.parse("2026-07-30T12:30:00Z");
        jdbcTemplate.update(
            """
                INSERT INTO job_postings (
                    id,
                    company_name,
                    role_title,
                    source,
                    source_url,
                    external_id,
                    work_mode,
                    found_on,
                    target_track,
                    classification,
                    description_snapshot,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            sourceUrl,
            externalId,
            "HYBRID",
            Date.valueOf(LocalDate.of(2026, 7, 29)),
            targetTrack,
            "A",
            descriptionSnapshot,
            Timestamp.from(timestamp),
            Timestamp.from(timestamp)
        );
    }
}
