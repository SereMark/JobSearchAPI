package hu.seregergo.jobsearch.jobapplication.api.validation;

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
@Constraint(validatedBy = ApplicationWorkflowValidator.class)
public @interface ValidApplicationWorkflow {

    String message() default "Application workflow is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
