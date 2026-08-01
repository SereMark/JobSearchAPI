package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository
    extends JpaRepository<JobApplication, UUID> {

    @Override
    @EntityGraph(attributePaths = "jobPosting")
    Optional<JobApplication> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "jobPosting")
    @Query("select application from JobApplication application where application.id = :id")
    Optional<JobApplication> findByIdForUpdate(@Param("id") UUID id);

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
