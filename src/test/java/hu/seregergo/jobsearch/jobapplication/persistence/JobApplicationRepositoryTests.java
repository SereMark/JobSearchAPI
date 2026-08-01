package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import hu.seregergo.jobsearch.jobposting.persistence.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class JobApplicationRepositoryTests extends PostgreSqlIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T08:00:00Z");

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void savesAndLoadsCompleteApplicationWithItsPostingSummary() {
        JobPosting posting = jobPostingRepository.saveAndFlush(jobPosting("Java role"));
        JobApplication application = JobApplication.create(
            posting,
            "Tailor the CV",
            LocalDate.of(2026, 8, 3),
            "Focus on Spring experience",
            CREATED_AT
        );

        JobApplication saved = applicationRepository.saveAndFlush(application);
        entityManager.clear();

        JobApplication loaded = applicationRepository.findById(saved.getId())
            .orElseThrow();

        assertAll(
            () -> assertNotNull(loaded.getId()),
            () -> assertEquals(posting.getId(), loaded.getJobPosting().getId()),
            () -> assertEquals("Java role", loaded.getJobPosting().getRoleTitle()),
            () -> assertEquals(ApplicationStage.PREPARING, loaded.getStage()),
            () -> assertNull(loaded.getSubmittedOn()),
            () -> assertEquals("Tailor the CV", loaded.getNextAction()),
            () -> assertEquals(LocalDate.of(2026, 8, 3), loaded.getDueOn()),
            () -> assertEquals("Focus on Spring experience", loaded.getNote()),
            () -> assertTrue(loaded.isActive()),
            () -> assertEquals(CREATED_AT, loaded.getCreatedAt()),
            () -> assertEquals(CREATED_AT, loaded.getUpdatedAt())
        );
    }

    @Test
    void databaseAllowsOnlyOneApplicationPerJobPosting() {
        JobPosting posting = jobPostingRepository.saveAndFlush(jobPosting("Java role"));
        applicationRepository.saveAndFlush(application(
            posting,
            "First action",
            LocalDate.of(2026, 8, 2),
            CREATED_AT
        ));

        JobApplication duplicate = application(
            posting,
            "Second action",
            LocalDate.of(2026, 8, 3),
            CREATED_AT.plusSeconds(1)
        );

        assertThrows(
            DataIntegrityViolationException.class,
            () -> applicationRepository.saveAndFlush(duplicate)
        );
    }

    @Test
    void databaseRejectsActiveApplicationWithoutOutstandingWork() {
        UUID postingId = savePostingForRawInsert();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawApplication(
                postingId,
                null,
                "PREPARING",
                null,
                null,
                null
            )
        );
    }

    @Test
    void databaseRejectsSubmissionDateThatContradictsStage() {
        UUID postingId = savePostingForRawInsert();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawApplication(
                postingId,
                LocalDate.of(2026, 8, 1),
                "PREPARING",
                "Finish the CV",
                LocalDate.of(2026, 8, 2),
                null
            )
        );
    }

    @Test
    void databaseRejectsSignedOutcomeOutsideOfferStage() {
        UUID postingId = savePostingForRawInsert();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawApplication(
                postingId,
                LocalDate.of(2026, 8, 1),
                "FINAL",
                null,
                null,
                "SIGNED"
            )
        );
    }

    @Test
    void filtersAndOrdersActiveDueAndClosedApplications() {
        JobApplication earliestDue = savedApplication(
            "Earliest due",
            LocalDate.of(2026, 8, 2),
            CREATED_AT
        );
        JobApplication cutoffDue = savedApplication(
            "Cutoff due",
            LocalDate.of(2026, 8, 3),
            CREATED_AT.plusSeconds(60)
        );
        JobApplication futureDue = savedApplication(
            "Future due",
            LocalDate.of(2026, 8, 4),
            CREATED_AT.plusSeconds(120)
        );
        JobApplication closed = savedApplication(
            "Closed",
            LocalDate.of(2026, 8, 1),
            CREATED_AT.plusSeconds(180)
        );
        closed.updateWorkflow(
            ApplicationStage.PREPARING,
            null,
            null,
            null,
            ApplicationOutcome.WITHDRAWN,
            "No longer pursuing the role",
            CREATED_AT.plusSeconds(300)
        );
        applicationRepository.flush();
        entityManager.clear();

        List<JobApplication> due = applicationRepository
            .findAllByOutcomeIsNullAndDueOnLessThanEqualOrderByDueOnAscUpdatedAtAscIdAsc(
                LocalDate.of(2026, 8, 3)
            );
        List<JobApplication> active = applicationRepository
            .findAllByOutcomeIsNullOrderByUpdatedAtDescIdAsc();
        List<JobApplication> closedApplications = applicationRepository
            .findAllByOutcomeIsNotNullOrderByUpdatedAtDescIdAsc();
        List<JobApplication> all = applicationRepository
            .findAllByOrderByUpdatedAtDescIdAsc();

        assertAll(
            () -> assertEquals(
                List.of(earliestDue.getId(), cutoffDue.getId()),
                ids(due)
            ),
            () -> assertEquals(
                List.of(futureDue.getId(), cutoffDue.getId(), earliestDue.getId()),
                ids(active)
            ),
            () -> assertEquals(List.of(closed.getId()), ids(closedApplications)),
            () -> assertEquals(closed.getId(), all.getFirst().getId()),
            () -> assertFalse(due.stream().anyMatch(
                application -> application.getId().equals(closed.getId())
            ))
        );
    }

    private JobApplication savedApplication(
        String roleTitle,
        LocalDate dueOn,
        Instant createdAt
    ) {
        JobPosting posting = jobPostingRepository.saveAndFlush(jobPosting(roleTitle));
        return applicationRepository.saveAndFlush(
            application(posting, "Next action for " + roleTitle, dueOn, createdAt)
        );
    }

    private JobApplication application(
        JobPosting posting,
        String nextAction,
        LocalDate dueOn,
        Instant createdAt
    ) {
        return JobApplication.create(posting, nextAction, dueOn, null, createdAt);
    }

    private List<UUID> ids(List<JobApplication> applications) {
        return applications.stream().map(JobApplication::getId).toList();
    }

    private UUID savePostingForRawInsert() {
        return jobPostingRepository.saveAndFlush(jobPosting("Raw insert role")).getId();
    }

    private void insertRawApplication(
        UUID postingId,
        LocalDate submittedOn,
        String stage,
        String nextAction,
        LocalDate dueOn,
        String outcome
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO applications (
                    id,
                    job_posting_id,
                    submitted_on,
                    stage,
                    next_action,
                    due_on,
                    outcome,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            postingId,
            submittedOn == null ? null : Date.valueOf(submittedOn),
            stage,
            nextAction,
            dueOn == null ? null : Date.valueOf(dueOn),
            outcome,
            Timestamp.from(CREATED_AT),
            Timestamp.from(CREATED_AT)
        );
    }

    private JobPosting jobPosting(String roleTitle) {
        return JobPosting.create(
            "Example Technologies Kft.",
            roleTitle,
            "Company careers",
            "https://example.com/jobs/" + UUID.randomUUID(),
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
