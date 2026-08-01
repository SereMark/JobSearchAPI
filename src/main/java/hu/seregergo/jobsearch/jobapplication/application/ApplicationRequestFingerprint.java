package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.IdempotencyOperation;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class ApplicationRequestFingerprint {

    private final ObjectMapper objectMapper;

    public ApplicationRequestFingerprint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String forSubmit(UUID applicationId, SubmitApplicationCommand command) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("operation", IdempotencyOperation.SUBMIT.name());
        canonical.put("applicationId", applicationId.toString());
        canonical.put("submittedOn", command.submittedOn().toString());
        canonical.put("nextAction", command.nextAction());
        canonical.put("dueOn", command.dueOn().toString());
        canonical.put("submittedCvIncluded", command.cv() != null);
        canonical.put(
            "cvLanguage",
            command.cvLanguage() == null ? null : command.cvLanguage().name()
        );
        canonical.put(
            "originalFileName",
            command.cv() == null ? null : command.cv().originalFileName()
        );
        canonical.put(
            "cvSha256",
            command.cv() == null ? null : command.cv().sha256()
        );
        return hash(canonical);
    }

    public String forRecordSentCv(
        UUID applicationId,
        RecordSentCvCommand command
    ) {
        Map<String, Object> canonical = new LinkedHashMap<>();
        canonical.put("operation", IdempotencyOperation.RECORD_SENT_CV.name());
        canonical.put("applicationId", applicationId.toString());
        canonical.put("sentOn", command.sentOn().toString());
        canonical.put("cvLanguage", command.cvLanguage().name());
        canonical.put("originalFileName", command.cv().originalFileName());
        canonical.put("cvSha256", command.cv().sha256());
        return hash(canonical);
    }

    private String hash(Map<String, Object> canonicalRequest) {
        try {
            byte[] canonicalJson = objectMapper.writeValueAsBytes(canonicalRequest);
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(canonicalJson)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "The request fingerprint could not be created",
                exception
            );
        }
    }
}
