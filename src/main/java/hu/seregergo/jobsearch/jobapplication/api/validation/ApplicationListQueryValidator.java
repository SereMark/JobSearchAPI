package hu.seregergo.jobsearch.jobapplication.api.validation;

import hu.seregergo.jobsearch.jobapplication.api.ApplicationListQuery;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ApplicationListQueryValidator
    implements ConstraintValidator<ValidApplicationListQuery, ApplicationListQuery> {

    @Override
    public boolean isValid(
        ApplicationListQuery query,
        ConstraintValidatorContext context
    ) {
        if (query == null
            || query.dueOnOrBefore() == null
            || Boolean.TRUE.equals(query.active())) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                "The active filter must be true when dueOnOrBefore is used"
            )
            .addPropertyNode("active")
            .addConstraintViolation();
        return false;
    }
}
