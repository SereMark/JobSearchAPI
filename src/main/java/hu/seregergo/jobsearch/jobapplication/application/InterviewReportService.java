package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.InterviewReport;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.persistence.InterviewReportRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class InterviewReportService {

    private final InterviewReportRepository reportRepository;
    private final JobApplicationRepository applicationRepository;
    private final Clock clock;

    public InterviewReportService(
        InterviewReportRepository reportRepository,
        JobApplicationRepository applicationRepository,
        Clock clock
    ) {
        this.reportRepository = reportRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    @Transactional
    public InterviewReport create(
        UUID applicationId,
        InterviewReportCommand command
    ) {
        JobApplication application = findApplication(applicationId);
        Instant timestamp = clock.instant();
        InterviewReport report = InterviewReport.create(
            application,
            command.interviewedOn(),
            command.roundLabel(),
            command.report(),
            localDate(timestamp),
            timestamp
        );
        return reportRepository.save(report);
    }

    public InterviewReport get(UUID applicationId, UUID reportId) {
        return findReport(applicationId, reportId);
    }

    public List<InterviewReport> list(UUID applicationId) {
        List<InterviewReport> reports = reportRepository
            .findAllByApplication_IdOrderByInterviewedOnDescCreatedAtDescIdDesc(
                applicationId
            );
        if (reports.isEmpty()) {
            ensureApplicationExists(applicationId);
        }
        return reports;
    }

    @Transactional
    public InterviewReport update(
        UUID applicationId,
        UUID reportId,
        InterviewReportCommand command
    ) {
        InterviewReport report = findReport(applicationId, reportId);
        Instant timestamp = clock.instant();
        report.update(
            command.interviewedOn(),
            command.roundLabel(),
            command.report(),
            localDate(timestamp),
            timestamp
        );
        return report;
    }

    private JobApplication findApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
            .orElseThrow(() -> new JobApplicationNotFoundException(applicationId));
    }

    private void ensureApplicationExists(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new JobApplicationNotFoundException(applicationId);
        }
    }

    private InterviewReport findReport(UUID applicationId, UUID reportId) {
        InterviewReport report = reportRepository
            .findByIdAndApplication_Id(reportId, applicationId)
            .orElse(null);
        if (report != null) {
            return report;
        }

        ensureApplicationExists(applicationId);
        throw new InterviewReportNotFoundException(applicationId, reportId);
    }

    private LocalDate localDate(Instant timestamp) {
        return LocalDate.ofInstant(timestamp, ZoneId.systemDefault());
    }
}
