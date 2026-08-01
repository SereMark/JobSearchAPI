package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationConflictException;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import hu.seregergo.jobsearch.jobposting.application.JobPostingNotFoundException;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.persistence.JobPostingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final Clock clock;

    public JobApplicationService(
        JobApplicationRepository applicationRepository,
        JobPostingRepository jobPostingRepository,
        Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.clock = clock;
    }

    @Transactional
    public JobApplication create(CreateApplicationCommand command) {
        JobPosting jobPosting = jobPostingRepository.findById(command.jobPostingId())
            .orElseThrow(() -> new JobPostingNotFoundException(command.jobPostingId()));

        if (jobPosting.getClassification() == JobPostingClassification.C) {
            throw ApplicationConflictException.ineligibleJobPosting();
        }
        if (applicationRepository.existsByJobPostingId(jobPosting.getId())) {
            throw ApplicationConflictException.alreadyExists();
        }

        JobApplication application = JobApplication.create(
            jobPosting,
            command.nextAction(),
            command.dueOn(),
            command.note(),
            clock.instant()
        );

        try {
            return applicationRepository.saveAndFlush(application);
        } catch (DataIntegrityViolationException exception) {
            throw ApplicationConflictException.alreadyExists(exception);
        }
    }

    public JobApplication get(UUID id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new JobApplicationNotFoundException(id));
    }

    public List<JobApplication> list(Boolean active, LocalDate dueOnOrBefore) {
        if (dueOnOrBefore != null) {
            if (!Boolean.TRUE.equals(active)) {
                throw new IllegalArgumentException(
                    "active must be true when dueOnOrBefore is used"
                );
            }
            return applicationRepository
                .findAllByOutcomeIsNullAndDueOnLessThanEqualOrderByDueOnAscUpdatedAtAscIdAsc(
                    dueOnOrBefore
                );
        }
        if (Boolean.TRUE.equals(active)) {
            return applicationRepository
                .findAllByOutcomeIsNullOrderByUpdatedAtDescIdAsc();
        }
        if (Boolean.FALSE.equals(active)) {
            return applicationRepository
                .findAllByOutcomeIsNotNullOrderByUpdatedAtDescIdAsc();
        }
        return applicationRepository.findAllByOrderByUpdatedAtDescIdAsc();
    }

    @Transactional
    public JobApplication updateWorkflow(
        UUID id,
        UpdateApplicationWorkflowCommand command
    ) {
        JobApplication application = get(id);
        application.updateWorkflow(
            command.stage(),
            command.stageLabel(),
            command.nextAction(),
            command.dueOn(),
            command.outcome(),
            command.note(),
            clock.instant()
        );
        return application;
    }

    @Transactional
    public JobApplication submit(UUID id, SubmitApplicationCommand command) {
        JobApplication application = get(id);
        application.submit(
            command.submittedOn(),
            command.nextAction(),
            command.dueOn(),
            LocalDate.now(clock.withZone(ZoneId.systemDefault())),
            clock.instant()
        );
        return application;
    }
}
