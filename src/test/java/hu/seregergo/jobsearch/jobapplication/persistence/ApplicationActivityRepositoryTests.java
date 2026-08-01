package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivity;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivityType;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApplicationActivityRepositoryTests extends PostgreSqlIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T12:00:00Z");

    @Autowired
    private ApplicationActivityRepository activityRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void clearDatabase() {
        activityRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void databaseRejectsAnUnknownActivityType() {
        JobApplication application = saveApplication("Unknown type");

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawActivity(
                application.getId(),
                "CHAT",
                "Recruiter replied",
                null,
                CREATED_AT.minusSeconds(60),
                CREATED_AT,
                CREATED_AT
            )
        );
    }

    @Test
    void databaseRejectsABlankSummary() {
        JobApplication application = saveApplication("Blank summary");

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawActivity(
                application.getId(),
                "EMAIL",
                "   ",
                null,
                CREATED_AT.minusSeconds(60),
                CREATED_AT,
                CREATED_AT
            )
        );
    }

    @Test
    void databaseRejectsBlankOptionalDetails() {
        JobApplication application = saveApplication("Blank details");

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawActivity(
                application.getId(),
                "EMAIL",
                "Recruiter replied",
                "   ",
                CREATED_AT.minusSeconds(60),
                CREATED_AT,
                CREATED_AT
            )
        );
    }

    @Test
    void databaseRejectsAnOccurrenceAfterTheUpdateTime() {
        JobApplication application = saveApplication("Invalid time");

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawActivity(
                application.getId(),
                "EMAIL",
                "Recruiter replied",
                null,
                CREATED_AT.plusSeconds(1),
                CREATED_AT,
                CREATED_AT
            )
        );
    }

    @Test
    void databaseRejectsAnUpdateBeforeCreation() {
        JobApplication application = saveApplication("Invalid update time");

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawActivity(
                application.getId(),
                "EMAIL",
                "Recruiter replied",
                null,
                CREATED_AT.minusSeconds(120),
                CREATED_AT,
                CREATED_AT.minusSeconds(60)
            )
        );
    }

    @Test
    void ordersActivitiesAndProjectsTheLatestOccurrencePerApplication() {
        JobApplication application = saveApplication("Timeline");
        JobApplication emptyApplication = saveApplication("No activity");
        ApplicationActivity older = activityRepository.saveAndFlush(
            ApplicationActivity.create(
                application,
                CREATED_AT.minusSeconds(3_600),
                ApplicationActivityType.EMAIL,
                "Recruiter replied",
                null,
                CREATED_AT
            )
        );
        ApplicationActivity newer = activityRepository.saveAndFlush(
            ApplicationActivity.create(
                application,
                CREATED_AT.minusSeconds(1_800),
                ApplicationActivityType.CALL,
                "Discussed the next round",
                null,
                CREATED_AT.plusSeconds(60)
            )
        );
        entityManager.clear();

        List<ApplicationActivity> activities = activityRepository
            .findAllByApplication_IdOrderByOccurredAtDescCreatedAtDescIdDesc(
                application.getId()
            );
        List<ApplicationLastActivityProjection> projections = activityRepository
            .findLastActivityAtByApplicationIdIn(List.of(
                application.getId(),
                emptyApplication.getId()
            ));

        assertEquals(List.of(newer.getId(), older.getId()), activities.stream()
            .map(ApplicationActivity::getId)
            .toList());
        assertEquals(1, projections.size());
        assertEquals(application.getId(), projections.getFirst().getApplicationId());
        assertEquals(
            CREATED_AT.minusSeconds(1_800),
            projections.getFirst().getLastActivityAt()
        );
    }

    @Test
    void deletingAnApplicationCascadesItsActivities() {
        JobApplication application = saveApplication("Cascade");
        ApplicationActivity activity = activityRepository.saveAndFlush(
            ApplicationActivity.create(
                application,
                CREATED_AT.minusSeconds(60),
                ApplicationActivityType.EMAIL,
                "Recruiter replied",
                null,
                CREATED_AT
            )
        );
        UUID activityId = activity.getId();
        UUID applicationId = application.getId();
        entityManager.clear();

        applicationRepository.deleteById(applicationId);
        applicationRepository.flush();
        entityManager.clear();

        assertFalse(activityRepository.existsById(activityId));
    }

    private JobApplication saveApplication(String roleTitle) {
        JobPosting posting = jobPostingRepository.saveAndFlush(JobPosting.create(
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
            CREATED_AT.minusSeconds(7_200)
        ));
        return applicationRepository.saveAndFlush(JobApplication.create(
            posting,
            "Prepare for the next step",
            LocalDate.of(2026, 8, 3),
            null,
            CREATED_AT.minusSeconds(7_200)
        ));
    }

    private void insertRawActivity(
        UUID applicationId,
        String type,
        String summary,
        String details,
        Instant occurredAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO application_activities (
                    id,
                    application_id,
                    occurred_at,
                    activity_type,
                    summary,
                    details,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            applicationId,
            Timestamp.from(occurredAt),
            type,
            summary,
            details,
            Timestamp.from(createdAt),
            Timestamp.from(updatedAt)
        );
    }
}
