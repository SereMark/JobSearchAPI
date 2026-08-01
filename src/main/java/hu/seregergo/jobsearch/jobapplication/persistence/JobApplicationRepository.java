package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository
    extends JpaRepository<JobApplication, UUID> {

    @Override
    @EntityGraph(attributePaths = "jobPosting")
    Optional<JobApplication> findById(UUID id);

    boolean existsByJobPostingId(UUID jobPostingId);

    @EntityGraph(attributePaths = "jobPosting")
    List<JobApplication> findAllByOrderByUpdatedAtDescIdAsc();

    @EntityGraph(attributePaths = "jobPosting")
    List<JobApplication> findAllByOutcomeIsNullOrderByUpdatedAtDescIdAsc();

    @EntityGraph(attributePaths = "jobPosting")
    List<JobApplication> findAllByOutcomeIsNotNullOrderByUpdatedAtDescIdAsc();

    @EntityGraph(attributePaths = "jobPosting")
    List<JobApplication>
        findAllByOutcomeIsNullAndDueOnLessThanEqualOrderByDueOnAscUpdatedAtAscIdAsc(
            LocalDate dueOn
        );
}
