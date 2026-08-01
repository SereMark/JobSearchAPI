package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationConflictException;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationIdempotencyRecord;
import hu.seregergo.jobsearch.jobapplication.domain.IdempotencyOperation;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationIdempotencyRecordRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class IdempotencyRecordResolver {

    private final ApplicationIdempotencyRecordRepository repository;

    public IdempotencyRecordResolver(
        ApplicationIdempotencyRecordRepository repository
    ) {
        this.repository = repository;
    }

    public Optional<StoredOperationResponse> replayIfPresent(
        UUID idempotencyKey,
        UUID applicationId,
        IdempotencyOperation operation,
        String requestFingerprint
    ) {
        return repository.findById(idempotencyKey)
            .map(record -> replay(
                record,
                applicationId,
                operation,
                requestFingerprint
            ));
    }

    public void requireOperationAvailable(
        UUID applicationId,
        IdempotencyOperation operation
    ) {
        repository.findByApplication_IdAndOperation(applicationId, operation)
            .ifPresent(ignored -> {
                throw ApplicationConflictException.idempotencyConflict(
                    "This operation has already been completed with a different "
                        + "idempotency key"
                );
            });
    }

    private StoredOperationResponse replay(
        ApplicationIdempotencyRecord record,
        UUID applicationId,
        IdempotencyOperation operation,
        String requestFingerprint
    ) {
        boolean sameRequest = applicationId.equals(record.getApplicationId())
            && operation == record.getOperation()
            && requestFingerprint.equals(record.getRequestFingerprint());
        if (!sameRequest) {
            throw ApplicationConflictException.idempotencyConflict(
                "The idempotency key was already used for a different request"
            );
        }
        return new StoredOperationResponse(
            record.getResponseStatus(),
            record.getResponseBody()
        );
    }
}
