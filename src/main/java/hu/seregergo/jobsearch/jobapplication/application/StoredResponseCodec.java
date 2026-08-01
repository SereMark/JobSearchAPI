package hu.seregergo.jobsearch.jobapplication.application;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class StoredResponseCodec {

    private final ObjectMapper objectMapper;

    public StoredResponseCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(Object response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception exception) {
            throw new IllegalStateException(
                "The idempotent response could not be stored",
                exception
            );
        }
    }
}
