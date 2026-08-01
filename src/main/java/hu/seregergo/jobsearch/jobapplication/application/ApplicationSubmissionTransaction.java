package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationConflictException;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationIdempotencyRecord;
import hu.seregergo.jobsearch.jobapplication.domain.IdempotencyOperation;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.domain.SubmittedCv;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationIdempotencyRecordRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationSubmissionTransaction {

    private static final int SUCCESS_STATUS = 200;

    private final JobApplicationRepository applicationRepository;
    private final SubmittedCvRepository cvRepository;
    private final ApplicationIdempotencyRecordRepository idempotencyRepository;
    private final IdempotencyRecordResolver idempotencyResolver;
    private final StoredResponseCodec responseCodec;
    private final Clock clock;

    public ApplicationSubmissionTransaction(
        JobApplicationRepository applicationRepository,
        SubmittedCvRepository cvRepository,
        ApplicationIdempotencyRecordRepository idempotencyRepository,
        IdempotencyRecordResolver idempotencyResolver,
        StoredResponseCodec responseCodec,
        Clock clock
    ) {
        this.applicationRepository = applicationRepository;
        this.cvRepository = cvRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.idempotencyResolver = idempotencyResolver;
        this.responseCodec = responseCodec;
        this.clock = clock;
    }

    @Transactional
    public StoredOperationResponse submit(
        UUID applicationId,
        UUID idempotencyKey,
        SubmitApplicationCommand command,
        String requestFingerprint
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

        JobApplication application = lockApplication(applicationId);
        replay = idempotencyResolver.replayIfPresent(
            idempotencyKey,
            applicationId,
            IdempotencyOperation.SUBMIT,
            requestFingerprint
        );
        if (replay.isPresent()) {
            return replay.get();
        }
        idempotencyResolver.requireOperationAvailable(
            applicationId,
            IdempotencyOperation.SUBMIT
        );

        Instant operationTime = clock.instant();
        LocalDate today = today();
        if (command.submittedOn().isAfter(today)) {
            throw new InvalidApplicationRequestException(
                "submittedOn must not be in the future"
            );
        }
        application.submit(
            command.submittedOn(),
            command.nextAction(),
            command.dueOn(),
            today,
            operationTime
        );

        SubmittedCvMetadata cvMetadata = null;
        if (command.cv() != null) {
            SubmittedCv cv = SubmittedCv.create(
                application,
                command.submittedOn(),
                command.cvLanguage(),
                command.cv(),
                today,
                operationTime
            );
            cvRepository.save(cv);
            cvMetadata = SubmittedCvMetadata.from(cv);
        }

        ApplicationSubmissionReceipt receipt = new ApplicationSubmissionReceipt(
            application.getId(),
            application.getStage(),
            application.getSubmittedOn(),
            application.getNextAction(),
            application.getDueOn(),
            application.getUpdatedAt(),
            cvMetadata
        );
        String responseBody = responseCodec.encode(receipt);
        storeRecord(
            idempotencyKey,
            application,
            IdempotencyOperation.SUBMIT,
            requestFingerprint,
            responseBody,
            operationTime
        );
        return new StoredOperationResponse(SUCCESS_STATUS, responseBody);
    }

    @Transactional
    public StoredOperationResponse recordSentCv(
        UUID applicationId,
        UUID idempotencyKey,
        RecordSentCvCommand command,
        String requestFingerprint
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

        JobApplication application = lockApplication(applicationId);
        replay = idempotencyResolver.replayIfPresent(
            idempotencyKey,
            applicationId,
            IdempotencyOperation.RECORD_SENT_CV,
            requestFingerprint
        );
        if (replay.isPresent()) {
            return replay.get();
        }
        idempotencyResolver.requireOperationAvailable(
            applicationId,
            IdempotencyOperation.RECORD_SENT_CV
        );

        if (!application.isActive() || application.getSubmittedOn() == null) {
            throw ApplicationConflictException.invalidTransition(
                "A CV can only be recorded for an active submitted application"
            );
        }
        if (cvRepository.existsById(applicationId)) {
            throw ApplicationConflictException.invalidTransition(
                "This application already has a submitted CV"
            );
        }

        LocalDate today = today();
        if (command.sentOn().isBefore(application.getSubmittedOn())) {
            throw new InvalidApplicationRequestException(
                "sentOn must not be before the application submission date"
            );
        }
        if (command.sentOn().isAfter(today)) {
            throw new InvalidApplicationRequestException(
                "sentOn must not be in the future"
            );
        }

        Instant operationTime = clock.instant();
        SubmittedCv cv = SubmittedCv.create(
            application,
            command.sentOn(),
            command.cvLanguage(),
            command.cv(),
            today,
            operationTime
        );
        cvRepository.save(cv);
        SubmittedCvMetadata metadata = SubmittedCvMetadata.from(cv);
        String responseBody = responseCodec.encode(metadata);
        storeRecord(
            idempotencyKey,
            application,
            IdempotencyOperation.RECORD_SENT_CV,
            requestFingerprint,
            responseBody,
            operationTime
        );
        return new StoredOperationResponse(SUCCESS_STATUS, responseBody);
    }

    private JobApplication lockApplication(UUID applicationId) {
        return applicationRepository.findByIdForUpdate(applicationId)
            .orElseThrow(() -> new JobApplicationNotFoundException(applicationId));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneId.systemDefault()));
    }

    private void storeRecord(
        UUID idempotencyKey,
        JobApplication application,
        IdempotencyOperation operation,
        String requestFingerprint,
        String responseBody,
        Instant createdAt
    ) {
        idempotencyRepository.saveAndFlush(
            ApplicationIdempotencyRecord.create(
                idempotencyKey,
                application,
                operation,
                requestFingerprint,
                SUCCESS_STATUS,
                responseBody,
                createdAt
            )
        );
    }
}
