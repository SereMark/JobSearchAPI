package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationActivityRepository
    extends JpaRepository<ApplicationActivity, UUID> {

    @EntityGraph(attributePaths = "application")
    List<ApplicationActivity>
        findAllByApplication_IdOrderByOccurredAtDescCreatedAtDescIdDesc(
            UUID applicationId
        );

    @EntityGraph(attributePaths = "application")
    Optional<ApplicationActivity> findByIdAndApplication_Id(
        UUID id,
        UUID applicationId
    );

    @Query("""
        SELECT
            activity.application.id AS applicationId,
            MAX(activity.occurredAt) AS lastActivityAt
        FROM ApplicationActivity activity
        WHERE activity.application.id IN :applicationIds
        GROUP BY activity.application.id
        """)
    List<ApplicationLastActivityProjection> findLastActivityAtByApplicationIdIn(
        @Param("applicationIds") Collection<UUID> applicationIds
    );
}
