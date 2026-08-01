package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.SubmittedCv;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SubmittedCvRepository extends JpaRepository<SubmittedCv, UUID> {

    @Query("""
        select cv.applicationId as applicationId,
               cv.sentOn as sentOn,
               cv.language as language,
               cv.originalFileName as originalFileName,
               cv.sizeBytes as sizeBytes,
               cv.sha256 as sha256,
               cv.recordedAt as recordedAt
        from SubmittedCv cv
        where cv.applicationId in :applicationIds
        """)
    List<SubmittedCvMetadataProjection> findMetadataByApplicationIdIn(
        @Param("applicationIds") Collection<UUID> applicationIds
    );
}
