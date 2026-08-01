package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.InterviewReport;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewReportRepository
    extends JpaRepository<InterviewReport, UUID> {

    @EntityGraph(attributePaths = "application")
    List<InterviewReport>
        findAllByApplication_IdOrderByInterviewedOnDescCreatedAtDescIdDesc(
            UUID applicationId
        );

    @EntityGraph(attributePaths = "application")
    Optional<InterviewReport> findByIdAndApplication_Id(
        UUID id,
        UUID applicationId
    );
}
