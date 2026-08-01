package hu.seregergo.jobsearch.jobapplication.api.validation;

import hu.seregergo.jobsearch.jobapplication.api.UpdateApplicationWorkflowRequest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ApplicationWorkflowValidator
    implements ConstraintValidator<
        ValidApplicationWorkflow,
        UpdateApplicationWorkflowRequest
    > {

    @Override
    public boolean isValid(
        UpdateApplicationWorkflowRequest request,
        ConstraintValidatorContext context
    ) {
        if (request == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (request.outcome() == null) {
            if (request.nextAction() == null) {
                addViolation(context, "nextAction", "Next action is required while active");
                valid = false;
            }
            if (request.dueOn() == null) {
                addViolation(context, "dueOn", "Due date is required while active");
                valid = false;
            }
        } else {
            if (request.nextAction() != null) {
                addViolation(
                    context,
                    "nextAction",
                    "Next action must be omitted when the application is closed"
                );
                valid = false;
            }
            if (request.dueOn() != null) {
                addViolation(
                    context,
                    "dueOn",
                    "Due date must be omitted when the application is closed"
                );
                valid = false;
            }
        }

        if ((request.outcome() == ApplicationOutcome.SIGNED
            || request.outcome() == ApplicationOutcome.OFFER_DECLINED)
            && request.stage() != null
            && request.stage() != ApplicationStage.OFFER) {
            addViolation(
                context,
                "outcome",
                "SIGNED and OFFER_DECLINED outcomes require the OFFER stage"
            );
            valid = false;
        }

        if (request.stage() == ApplicationStage.PREPARING
            && request.outcome() != null
            && request.outcome() != ApplicationOutcome.WITHDRAWN
            && request.outcome() != ApplicationOutcome.ROLE_CANCELLED) {
            addViolation(
                context,
                "outcome",
                "A PREPARING application can only close as WITHDRAWN or ROLE_CANCELLED"
            );
            valid = false;
        }

        return valid;
    }

    private void addViolation(
        ConstraintValidatorContext context,
        String field,
        String message
    ) {
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(field)
            .addConstraintViolation();
    }
}
