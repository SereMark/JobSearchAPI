package hu.seregergo.jobsearch.jobposting.api.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CreateJobPostingRequestValidator.class)
public @interface ValidCreateJobPostingRequest {

    String message() default "Job posting request is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
