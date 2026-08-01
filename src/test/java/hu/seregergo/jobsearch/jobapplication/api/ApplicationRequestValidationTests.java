package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationRequestValidationTests {

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
    void acceptsValidCreateSubmitActiveClosedAndDueQueryRequests() {
        assertAllValid(
            new CreateApplicationRequest(
                UUID.randomUUID(),
                "Tailor the CV",
                LocalDate.now(),
                null
            ),
            new SubmitApplicationRequest(
                LocalDate.now(),
                "Check for a response",
                LocalDate.now().plusDays(7)
            ),
            new UpdateApplicationWorkflowRequest(
                ApplicationStage.TECHNICAL_INTERVIEW,
                "Technical call",
                "Prepare examples",
                LocalDate.now().plusDays(3),
                null,
                null
            ),
            new UpdateApplicationWorkflowRequest(
                ApplicationStage.OFFER,
                null,
                null,
                null,
                ApplicationOutcome.SIGNED,
                "Contract signed"
            ),
            new ApplicationListQuery(true, LocalDate.now())
        );
    }

    @Test
    void rejectsMissingAndBlankCreateFields() {
        CreateApplicationRequest request = new CreateApplicationRequest(
            null,
            "   ",
            null,
            "   "
        );

        assertEquals(
            Set.of("jobPostingId", "nextAction", "dueOn", "note"),
            violatedFields(request)
        );
    }

    @Test
    void activeWorkflowRequiresNextActionAndDueDate() {
        UpdateApplicationWorkflowRequest request = new UpdateApplicationWorkflowRequest(
            ApplicationStage.RECRUITER_SCREEN,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals(Set.of("nextAction", "dueOn"), violatedFields(request));
    }

    @Test
    void closedWorkflowRejectsOutstandingWork() {
        UpdateApplicationWorkflowRequest request = new UpdateApplicationWorkflowRequest(
            ApplicationStage.TECHNICAL_INTERVIEW,
            null,
            "Send a follow-up",
            LocalDate.now(),
            ApplicationOutcome.REJECTED,
            null
        );

        assertEquals(Set.of("nextAction", "dueOn"), violatedFields(request));
    }

    @Test
    void rejectsOutcomesThatDoNotMatchTheirStage() {
        UpdateApplicationWorkflowRequest signedBeforeOffer =
            new UpdateApplicationWorkflowRequest(
                ApplicationStage.FINAL,
                null,
                null,
                null,
                ApplicationOutcome.SIGNED,
                null
            );
        UpdateApplicationWorkflowRequest rejectedBeforeSubmission =
            new UpdateApplicationWorkflowRequest(
                ApplicationStage.PREPARING,
                null,
                null,
                null,
                ApplicationOutcome.REJECTED,
                null
            );

        assertEquals(Set.of("outcome"), violatedFields(signedBeforeOffer));
        assertEquals(Set.of("outcome"), violatedFields(rejectedBeforeSubmission));
    }

    @Test
    void dueDateCutoffRequiresExplicitActiveFilter() {
        assertEquals(
            Set.of("active"),
            violatedFields(new ApplicationListQuery(null, LocalDate.now()))
        );
        assertEquals(
            Set.of("active"),
            violatedFields(new ApplicationListQuery(false, LocalDate.now()))
        );
    }

    @Test
    void submissionDateCannotBeInTheFuture() {
        SubmitApplicationRequest request = new SubmitApplicationRequest(
            LocalDate.now().plusDays(1),
            "Check for a response",
            LocalDate.now().plusDays(7)
        );

        assertEquals(Set.of("submittedOn"), violatedFields(request));
    }

    private void assertAllValid(Object... requests) {
        for (Object request : requests) {
            assertTrue(validator.validate(request).isEmpty());
        }
    }

    private Set<String> violatedFields(Object request) {
        return validator.validate(request)
            .stream()
            .map(violation -> violation.getPropertyPath().toString())
            .collect(Collectors.toSet());
    }
}
