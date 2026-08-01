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
@Constraint(validatedBy = ApplicationListQueryValidator.class)
public @interface ValidApplicationListQuery {

    String message() default "Application list query is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
