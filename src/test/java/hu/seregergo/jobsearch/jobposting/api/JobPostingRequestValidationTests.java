package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
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

class JobPostingRequestValidationTests {

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
    void acceptsValidCreateAndUpdateRequests() {
        CreateJobPostingRequest createRequest = validCreateRequest(
            "https://careers.example.com/jobs/123",
            null,
            JobPostingClassification.A,
            null
        );
        UpdateJobPostingRequest updateRequest = validUpdateRequest(
            null,
            "JOB-123",
            JobPostingClassification.B,
            "One optional technology is missing"
        );

        assertTrue(validator.validate(createRequest).isEmpty());
        assertTrue(validator.validate(updateRequest).isEmpty());
    }

    @Test
    void rejectsMissingSourceReferenceAndMissingNoteForCClassification() {
        CreateJobPostingRequest request = validCreateRequest(
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
        UpdateJobPostingRequest request = new UpdateJobPostingRequest(
            "   ",
            "r".repeat(201),
            "",
            "ftp://example.com/jobs/123",
            null,
            "   ",
            null,
            LocalDate.of(2999, 1, 1),
            null,
            JobPostingClassification.A,
            null,
            "   "
        );

        assertEquals(
            Set.of(
                "companyName",
                "roleTitle",
                "source",
                "sourceUrl",
                "location",
                "workMode",
                "foundOn",
                "targetTrack",
                "descriptionSnapshot"
            ),
            violatedFields(request)
        );
    }

    @Test
    void rejectsDescriptionSnapshotOverLimit() {
        CreateJobPostingRequest request = new CreateJobPostingRequest(
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            "https://careers.example.com/jobs/123",
            null,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2020, 1, 15),
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            "x".repeat(50_001)
        );

        assertEquals(Set.of("descriptionSnapshot"), violatedFields(request));
    }

    private CreateJobPostingRequest validCreateRequest(
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
            TargetTrack.JAVA,
            classification,
            reviewNote,
            "We are looking for a Java backend developer."
        );
    }

    private UpdateJobPostingRequest validUpdateRequest(
        String sourceUrl,
        String externalId,
        JobPostingClassification classification,
        String reviewNote
    ) {
        return new UpdateJobPostingRequest(
            "Example Technologies Kft.",
            "Java Backend Developer",
            "Company careers",
            sourceUrl,
            externalId,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2020, 1, 15),
            TargetTrack.JAVA,
            classification,
            reviewNote,
            null
        );
    }

    private Set<String> violatedFields(Object request) {
        return validator.validate(request)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
