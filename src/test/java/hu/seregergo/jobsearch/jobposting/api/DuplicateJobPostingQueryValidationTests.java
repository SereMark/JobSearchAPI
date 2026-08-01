package hu.seregergo.jobsearch.jobposting.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateJobPostingQueryValidationTests {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void acceptsEitherSourceReference() {
        assertTrue(validator.validate(new DuplicateJobPostingQuery(
            "https://careers.example.com/jobs/123",
            null,
            null
        )).isEmpty());
        assertTrue(validator.validate(new DuplicateJobPostingQuery(
            null,
            "JOB-123",
            null
        )).isEmpty());
    }

    @Test
    void rejectsMissingOrInvalidSourceReference() {
        assertEquals(
            Set.of("sourceUrl"),
            violatedFields(new DuplicateJobPostingQuery(null, null, null))
        );
        assertEquals(
            Set.of("sourceUrl", "externalId"),
            violatedFields(new DuplicateJobPostingQuery(
                "ftp://example.com/job",
                "   ",
                null
            ))
        );
    }

    private Set<String> violatedFields(DuplicateJobPostingQuery query) {
        return validator.validate(query)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
