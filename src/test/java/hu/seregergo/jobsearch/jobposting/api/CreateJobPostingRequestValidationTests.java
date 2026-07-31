package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateJobPostingRequestValidationTests {

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
    void acceptsRequestWithSourceUrl() {
        CreateJobPostingRequest request = validRequest(
            "https://careers.example.com/jobs/123",
            null,
            JobPostingClassification.A,
            null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void acceptsRequestWithExternalId() {
        CreateJobPostingRequest request = validRequest(
            null,
            "JOB-123",
            JobPostingClassification.B,
            "One optional technology is missing"
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsMissingSourceReferenceAndMissingNoteForCClassification() {
        CreateJobPostingRequest request = validRequest(
            null,
            null,
            JobPostingClassification.C,
            null
        );

        assertEquals(
            Set.of("sourceUrl", "reviewNote"),
            violatedFields(request)
        );
    }

    @Test
    void rejectsInvalidSingleFieldValues() {
        CreateJobPostingRequest request = new CreateJobPostingRequest(
            "   ",
            "r".repeat(201),
            "",
            "ftp://example.com/jobs/123",
            null,
            "   ",
            null,
            LocalDate.of(2999, 1, 1),
            JobPostingClassification.A,
            null
        );

        assertEquals(
            Set.of(
                "companyName",
                "roleTitle",
                "source",
                "sourceUrl",
                "location",
                "workMode",
                "foundOn"
            ),
            violatedFields(request)
        );
    }

    private CreateJobPostingRequest validRequest(
        String sourceUrl,
        String externalId,
        JobPostingClassification classification,
        String reviewNote
    ) {
        return new CreateJobPostingRequest(
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            sourceUrl,
            externalId,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2020, 1, 15),
            classification,
            reviewNote
        );
    }

    private Set<String> violatedFields(CreateJobPostingRequest request) {
        return validator.validate(request)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
