package hu.seregergo.jobsearch.jobapplication.persistence;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationIdempotencyRecord;
import hu.seregergo.jobsearch.jobapplication.domain.IdempotencyOperation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApplicationIdempotencyRecordRepository
    extends JpaRepository<ApplicationIdempotencyRecord, UUID> {

    Optional<ApplicationIdempotencyRecord> findByApplication_IdAndOperation(
        UUID applicationId,
        IdempotencyOperation operation
    );
}
