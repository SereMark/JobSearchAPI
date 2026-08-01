package hu.seregergo.jobsearch.jobposting.api.validation;

import hu.seregergo.jobsearch.jobposting.api.DuplicateJobPostingQuery;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DuplicateJobPostingQueryValidator
    implements ConstraintValidator<ValidDuplicateJobPostingQuery, DuplicateJobPostingQuery> {

    @Override
    public boolean isValid(
        DuplicateJobPostingQuery query,
        ConstraintValidatorContext context
    ) {
        if (query == null || query.sourceUrl() != null || query.externalId() != null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "Source URL or external ID is required"
            )
            .addPropertyNode("sourceUrl")
            .addConstraintViolation();
        return false;
    }
}
