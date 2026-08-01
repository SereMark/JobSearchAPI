package hu.seregergo.jobsearch.jobposting.api.validation;

import hu.seregergo.jobsearch.jobposting.api.JobPostingRequest;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class JobPostingRequestValidator
    implements ConstraintValidator<ValidJobPostingRequest, JobPostingRequest> {

    @Override
    public boolean isValid(
        JobPostingRequest request,
        ConstraintValidatorContext context
    ) {
        if (request == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (request.sourceUrl() == null && request.externalId() == null) {
            addViolation(
                context,
                "Source URL or external ID is required",
                "sourceUrl"
            );
            valid = false;
        }

        if (request.classification() == JobPostingClassification.C
            && request.reviewNote() == null) {
            addViolation(
                context,
                "Review note is required for C classification",
                "reviewNote"
            );
            valid = false;
        }

        return valid;
    }

    private void addViolation(
        ConstraintValidatorContext context,
        String message,
        String property
    ) {
        context.buildConstraintViolationWithTemplate(message)
            .addPropertyNode(property)
            .addConstraintViolation();
    }
}
