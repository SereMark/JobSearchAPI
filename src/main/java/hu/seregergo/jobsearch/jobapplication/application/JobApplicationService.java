package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationConflictException;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvMetadataProjection;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvRepository;
import hu.seregergo.jobsearch.jobposting.application.JobPostingNotFoundException;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.persistence.JobPostingRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final SubmittedCvRepository cvRepository;
    private final Clock clock;

    public JobApplicationService(
        JobApplicationRepository applicationRepository,
        JobPostingRepository jobPostingRepository,
        SubmittedCvRepository cvRepository,
        Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.cvRepository = cvRepository;
        this.clock = clock;
    }

    @Transactional
    public JobApplicationDetails create(CreateApplicationCommand command) {
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
            JobApplication saved = applicationRepository.saveAndFlush(application);
            return new JobApplicationDetails(saved, null);
        } catch (DataIntegrityViolationException exception) {
            throw ApplicationConflictException.alreadyExists(exception);
        }
    }

    public JobApplicationDetails get(UUID id) {
        JobApplication application = findApplication(id);
        return detailsFor(List.of(application)).getFirst();
    }

    public List<JobApplicationDetails> list(Boolean active, LocalDate dueOnOrBefore) {
        List<JobApplication> applications;
        if (dueOnOrBefore != null) {
            if (!Boolean.TRUE.equals(active)) {
                throw new IllegalArgumentException(
                    "active must be true when dueOnOrBefore is used"
                );
            }
            applications = applicationRepository
                .findAllByOutcomeIsNullAndDueOnLessThanEqualOrderByDueOnAscUpdatedAtAscIdAsc(
                    dueOnOrBefore
                );
        } else if (Boolean.TRUE.equals(active)) {
            applications = applicationRepository
                .findAllByOutcomeIsNullOrderByUpdatedAtDescIdAsc();
        } else if (Boolean.FALSE.equals(active)) {
            applications = applicationRepository
                .findAllByOutcomeIsNotNullOrderByUpdatedAtDescIdAsc();
        } else {
            applications = applicationRepository.findAllByOrderByUpdatedAtDescIdAsc();
        }
        return detailsFor(applications);
    }

    @Transactional
    public JobApplicationDetails updateWorkflow(
        UUID id,
        UpdateApplicationWorkflowCommand command
    ) {
        JobApplication application = applicationRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new JobApplicationNotFoundException(id));
        application.updateWorkflow(
            command.stage(),
            command.stageLabel(),
            command.nextAction(),
            command.dueOn(),
            command.outcome(),
            command.note(),
            clock.instant()
        );
        return detailsFor(List.of(application)).getFirst();
    }

    private JobApplication findApplication(UUID id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new JobApplicationNotFoundException(id));
    }

    private List<JobApplicationDetails> detailsFor(
        List<JobApplication> applications
    ) {
        if (applications.isEmpty()) {
            return List.of();
        }
        Collection<UUID> applicationIds = applications.stream()
            .map(JobApplication::getId)
            .toList();
        Map<UUID, SubmittedCvMetadataProjection> metadataByApplicationId = cvRepository
            .findMetadataByApplicationIdIn(applicationIds)
            .stream()
            .collect(Collectors.toMap(
                SubmittedCvMetadataProjection::getApplicationId,
                Function.identity()
            ));

        return applications.stream()
            .map(application -> new JobApplicationDetails(
                application,
                metadataFor(application.getId(), metadataByApplicationId)
            ))
            .toList();
    }

    private SubmittedCvMetadata metadataFor(
        UUID applicationId,
        Map<UUID, SubmittedCvMetadataProjection> metadataByApplicationId
    ) {
        SubmittedCvMetadataProjection metadata = metadataByApplicationId.get(applicationId);
        return metadata == null ? null : SubmittedCvMetadata.from(metadata);
    }
}
