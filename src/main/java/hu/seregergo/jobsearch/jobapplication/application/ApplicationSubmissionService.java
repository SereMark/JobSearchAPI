package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationConflictException;
import hu.seregergo.jobsearch.jobapplication.domain.IdempotencyOperation;
import hu.seregergo.jobsearch.jobapplication.domain.SubmittedCv;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationIdempotencyRecordRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationSubmissionService {

    private final JobApplicationRepository applicationRepository;
    private final SubmittedCvRepository cvRepository;
    private final ApplicationIdempotencyRecordRepository idempotencyRepository;
    private final ApplicationRequestFingerprint fingerprint;
    private final IdempotencyRecordResolver idempotencyResolver;
    private final ApplicationSubmissionTransaction transaction;

    public ApplicationSubmissionService(
        JobApplicationRepository applicationRepository,
        SubmittedCvRepository cvRepository,
        ApplicationIdempotencyRecordRepository idempotencyRepository,
        ApplicationRequestFingerprint fingerprint,
        IdempotencyRecordResolver idempotencyResolver,
        ApplicationSubmissionTransaction transaction
    ) {
        this.applicationRepository = applicationRepository;
        this.cvRepository = cvRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.fingerprint = fingerprint;
        this.idempotencyResolver = idempotencyResolver;
        this.transaction = transaction;
    }

    public StoredOperationResponse submit(
        UUID applicationId,
        UUID idempotencyKey,
        SubmitApplicationCommand command
    ) {
        String requestFingerprint = fingerprint.forSubmit(applicationId, command);
        Optional<StoredOperationResponse> replay = idempotencyResolver
            .replayIfPresent(
                idempotencyKey,
                applicationId,
                IdempotencyOperation.SUBMIT,
                requestFingerprint
            );
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            return transaction.submit(
                applicationId,
                idempotencyKey,
                command,
                requestFingerprint
            );
        } catch (DataIntegrityViolationException exception) {
            return resolveSubmitRace(
                applicationId,
                idempotencyKey,
                requestFingerprint,
                exception
            );
        }
    }

    public StoredOperationResponse recordSentCv(
        UUID applicationId,
        UUID idempotencyKey,
        RecordSentCvCommand command
    ) {
        String requestFingerprint = fingerprint.forRecordSentCv(applicationId, command);
        Optional<StoredOperationResponse> replay = idempotencyResolver.replayIfPresent(
            idempotencyKey,
            applicationId,
            IdempotencyOperation.RECORD_SENT_CV,
            requestFingerprint
        );
        if (replay.isPresent()) {
            return replay.get();
        }

        try {
            return transaction.recordSentCv(
                applicationId,
                idempotencyKey,
                command,
                requestFingerprint
            );
        } catch (DataIntegrityViolationException exception) {
            return resolveRecordCvRace(
                applicationId,
                idempotencyKey,
                requestFingerprint,
                exception
            );
        }
    }

    @Transactional(readOnly = true)
    public SubmittedCvDownload download(UUID applicationId) {
        if (!applicationRepository.existsById(applicationId)) {
            throw new JobApplicationNotFoundException(applicationId);
        }
        SubmittedCv cv = cvRepository.findById(applicationId)
            .orElseThrow(() -> new SubmittedCvNotFoundException(applicationId));
        return new SubmittedCvDownload(cv.getOriginalFileName(), cv.getBytes());
    }

    private StoredOperationResponse resolveSubmitRace(
        UUID applicationId,
        UUID idempotencyKey,
        String requestFingerprint,
        DataIntegrityViolationException exception
    ) {
        Optional<StoredOperationResponse> replay = idempotencyResolver
            .replayIfPresent(
                idempotencyKey,
                applicationId,
                IdempotencyOperation.SUBMIT,
                requestFingerprint
            );
        if (replay.isPresent()) {
            return replay.get();
        }
        return failAfterRace(applicationId, IdempotencyOperation.SUBMIT, exception);
    }

    private StoredOperationResponse resolveRecordCvRace(
        UUID applicationId,
        UUID idempotencyKey,
        String requestFingerprint,
        DataIntegrityViolationException exception
    ) {
        Optional<StoredOperationResponse> replay = idempotencyResolver.replayIfPresent(
            idempotencyKey,
            applicationId,
            IdempotencyOperation.RECORD_SENT_CV,
            requestFingerprint
        );
        if (replay.isPresent()) {
            return replay.get();
        }
        return failAfterRace(
            applicationId,
            IdempotencyOperation.RECORD_SENT_CV,
            exception
        );
    }

    private StoredOperationResponse failAfterRace(
        UUID applicationId,
        IdempotencyOperation operation,
        DataIntegrityViolationException exception
    ) {
        if (idempotencyRepository
            .findByApplication_IdAndOperation(applicationId, operation)
            .isPresent()) {
            throw ApplicationConflictException.idempotencyConflict(
                "This operation has already been completed with a different "
                    + "idempotency key"
            );
        }
        throw exception;
    }
}
