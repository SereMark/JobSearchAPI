package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivity;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationActivityRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ApplicationActivityService {

    private final ApplicationActivityRepository activityRepository;
    private final JobApplicationRepository applicationRepository;
    private final Clock clock;

    public ApplicationActivityService(
        ApplicationActivityRepository activityRepository,
        JobApplicationRepository applicationRepository,
        Clock clock
    ) {
        this.activityRepository = activityRepository;
        this.applicationRepository = applicationRepository;
        this.clock = clock;
    }

    @Transactional
    public ApplicationActivity create(
        UUID applicationId,
        ApplicationActivityCommand command
    ) {
        JobApplication application = findApplication(applicationId);
        Instant timestamp = clock.instant();
        ApplicationActivity activity = ApplicationActivity.create(
            application,
            command.occurredAt(),
            command.type(),
            command.summary(),
            command.details(),
            timestamp
        );
        return activityRepository.save(activity);
    }

    public ApplicationActivity get(UUID applicationId, UUID activityId) {
        return findActivity(applicationId, activityId);
    }

    public List<ApplicationActivity> list(UUID applicationId) {
        List<ApplicationActivity> activities = activityRepository
            .findAllByApplication_IdOrderByOccurredAtDescCreatedAtDescIdDesc(
                applicationId
            );
        if (activities.isEmpty()) {
            ensureApplicationExists(applicationId);
        }
        return activities;
    }

    @Transactional
    public ApplicationActivity update(
        UUID applicationId,
        UUID activityId,
        ApplicationActivityCommand command
    ) {
        ApplicationActivity activity = findActivity(applicationId, activityId);
        activity.update(
            command.occurredAt(),
            command.type(),
            command.summary(),
            command.details(),
            clock.instant()
        );
        return activity;
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

    private ApplicationActivity findActivity(
        UUID applicationId,
        UUID activityId
    ) {
        ApplicationActivity activity = activityRepository
            .findByIdAndApplication_Id(activityId, applicationId)
            .orElse(null);
        if (activity != null) {
            return activity;
        }

        ensureApplicationExists(applicationId);
        throw new ApplicationActivityNotFoundException(applicationId, activityId);
    }
}
