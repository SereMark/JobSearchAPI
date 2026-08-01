package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.InterviewReport;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class InterviewReportRepositoryTests extends PostgreSqlIntegrationTest {

    private static final Instant CREATED_AT = Instant.parse("2026-08-01T08:00:00Z");

    @Autowired
    private InterviewReportRepository reportRepository;

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
        reportRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void databaseRejectsBlankInterviewText() {
        JobApplication application = saveApplication();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawReport(
                application.getId(),
                "Recruiter screen",
                "   ",
                CREATED_AT,
                CREATED_AT
            )
        );
    }

    @Test
    void databaseRejectsAnUpdateTimestampBeforeCreation() {
        JobApplication application = saveApplication();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertRawReport(
                application.getId(),
                "Recruiter screen",
                "Useful conversation",
                CREATED_AT,
                CREATED_AT.minusSeconds(1)
            )
        );
    }

    @Test
    void deletingAnApplicationCascadesItsInterviewReports() {
        JobApplication application = saveApplication();
        InterviewReport report = reportRepository.saveAndFlush(InterviewReport.create(
            application,
            LocalDate.of(2026, 8, 1),
            "Recruiter screen",
            "Useful conversation",
            LocalDate.of(2026, 8, 1),
            CREATED_AT
        ));
        UUID reportId = report.getId();
        UUID applicationId = application.getId();
        entityManager.clear();

        applicationRepository.deleteById(applicationId);
        applicationRepository.flush();
        entityManager.clear();

        assertFalse(reportRepository.existsById(reportId));
    }

    private JobApplication saveApplication() {
        JobPosting posting = jobPostingRepository.saveAndFlush(JobPosting.create(
            "Example Technologies Kft.",
            "Java Backend Developer",
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
        ));
        return applicationRepository.saveAndFlush(JobApplication.create(
            posting,
            "Prepare for the interview",
            LocalDate.of(2026, 8, 2),
            null,
            CREATED_AT
        ));
    }

    private void insertRawReport(
        UUID applicationId,
        String roundLabel,
        String report,
        Instant createdAt,
        Instant updatedAt
    ) {
        jdbcTemplate.update(
            """
                INSERT INTO interview_reports (
                    id,
                    application_id,
                    interviewed_on,
                    round_label,
                    report,
                    created_at,
                    updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
            UUID.randomUUID(),
            applicationId,
            Date.valueOf(LocalDate.of(2026, 8, 1)),
            roundLabel,
            report,
            Timestamp.from(createdAt),
            Timestamp.from(updatedAt)
        );
    }
}
