package hu.seregergo.jobsearch.jobposting.application;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.persistence.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class JobPostingService {

    private final JobPostingRepository repository;
    private final Clock clock;

    public JobPostingService(JobPostingRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public JobPosting create(CreateJobPostingCommand command) {
        JobPosting jobPosting = JobPosting.create(
            command.companyName(),
            command.roleTitle(),
            command.source(),
            command.sourceUrl(),
            command.externalId(),
            command.location(),
            command.workMode(),
            command.foundOn(),
            command.targetTrack(),
            command.classification(),
            command.reviewNote(),
            command.descriptionSnapshot(),
            clock.instant()
        );

        return repository.save(jobPosting);
    }

    public JobPosting get(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new JobPostingNotFoundException(id));
    }

    @Transactional
    public JobPosting update(UUID id, UpdateJobPostingCommand command) {
        JobPosting jobPosting = get(id);
        jobPosting.update(
            command.companyName(),
            command.roleTitle(),
            command.source(),
            command.sourceUrl(),
            command.externalId(),
            command.location(),
            command.workMode(),
            command.foundOn(),
            command.targetTrack(),
            command.classification(),
            command.reviewNote(),
            command.descriptionSnapshot(),
            clock.instant()
        );
        return jobPosting;
    }

    public List<JobPosting> list(
        TargetTrack targetTrack,
        JobPostingClassification classification
    ) {
        if (targetTrack != null && classification != null) {
            return repository
                .findAllByTargetTrackAndClassificationOrderByCreatedAtDescIdDesc(
                    targetTrack,
                    classification
                );
        }
        if (targetTrack != null) {
            return repository.findAllByTargetTrackOrderByCreatedAtDescIdDesc(
                targetTrack
            );
        }
        if (classification != null) {
            return repository.findAllByClassificationOrderByCreatedAtDescIdDesc(
                classification
            );
        }
        return repository.findAllByOrderByCreatedAtDescIdDesc();
    }

    public List<JobPosting> findDuplicateCandidates(
        String sourceUrl,
        String externalId,
        UUID excludeId
    ) {
        String normalizedSourceUrl = normalizeOptionalText(sourceUrl);
        String normalizedExternalId = normalizeOptionalText(externalId);

        List<JobPosting> candidates;
        if (normalizedSourceUrl != null && normalizedExternalId != null) {
            candidates = repository
                .findAllBySourceUrlOrExternalIdOrderByCreatedAtDescIdDesc(
                    normalizedSourceUrl,
                    normalizedExternalId
                );
        } else if (normalizedSourceUrl != null) {
            candidates = repository.findAllBySourceUrlOrderByCreatedAtDescIdDesc(
                normalizedSourceUrl
            );
        } else if (normalizedExternalId != null) {
            candidates = repository.findAllByExternalIdOrderByCreatedAtDescIdDesc(
                normalizedExternalId
            );
        } else {
            throw new IllegalArgumentException("sourceUrl or externalId is required");
        }

        if (excludeId == null) {
            return candidates;
        }
        return candidates.stream()
            .filter(candidate -> !excludeId.equals(candidate.getId()))
            .toList();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }
}
