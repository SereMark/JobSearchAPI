package hu.seregergo.jobsearch.jobposting.application;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
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
            command.classification(),
            command.reviewNote(),
            clock.instant()
        );

        return repository.save(jobPosting);
    }

    public JobPosting get(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new JobPostingNotFoundException(id));
    }

    public List<JobPosting> list() {
        return repository.findAllByOrderByCreatedAtDescIdDesc();
    }
}
