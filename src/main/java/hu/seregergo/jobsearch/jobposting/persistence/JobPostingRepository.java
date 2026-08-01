package hu.seregergo.jobsearch.jobposting.persistence;

import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    List<JobPosting> findAllByOrderByCreatedAtDescIdDesc();

    List<JobPosting> findAllByTargetTrackOrderByCreatedAtDescIdDesc(
        TargetTrack targetTrack
    );

    List<JobPosting> findAllByClassificationOrderByCreatedAtDescIdDesc(
        JobPostingClassification classification
    );

    List<JobPosting> findAllByTargetTrackAndClassificationOrderByCreatedAtDescIdDesc(
        TargetTrack targetTrack,
        JobPostingClassification classification
    );

    List<JobPosting> findAllBySourceUrlOrderByCreatedAtDescIdDesc(String sourceUrl);

    List<JobPosting> findAllByExternalIdOrderByCreatedAtDescIdDesc(String externalId);

    List<JobPosting> findAllBySourceUrlOrExternalIdOrderByCreatedAtDescIdDesc(
        String sourceUrl,
        String externalId
    );
}
