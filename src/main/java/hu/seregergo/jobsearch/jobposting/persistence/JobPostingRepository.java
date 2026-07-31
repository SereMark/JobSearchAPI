package hu.seregergo.jobsearch.jobposting.persistence;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    List<JobPosting> findAllByOrderByCreatedAtDescIdDesc();
}
