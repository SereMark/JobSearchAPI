package hu.seregergo.jobsearch.common.api;

import hu.seregergo.jobsearch.jobapplication.application.InterviewReportNotFoundException;
import hu.seregergo.jobsearch.jobapplication.application.InvalidApplicationRequestException;
import hu.seregergo.jobsearch.jobapplication.application.InvalidSubmittedCvException;
import hu.seregergo.jobsearch.jobapplication.application.JobApplicationNotFoundException;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvNotFoundException;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationConflictException;
import hu.seregergo.jobsearch.jobposting.application.JobPostingNotFoundException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InterviewReportNotFoundException.class)
    ResponseEntity<Object> handleInterviewReportNotFound(
        InterviewReportNotFoundException exception,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.NOT_FOUND,
            "Interview report not found",
            exception.getMessage(),
            "INTERVIEW_REPORT_NOT_FOUND",
            "interview-report-not-found",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request
        );
    }

    @ExceptionHandler(JobApplicationNotFoundException.class)
    ResponseEntity<Object> handleJobApplicationNotFound(
        JobApplicationNotFoundException exception,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.NOT_FOUND,
            "Application not found",
            exception.getMessage(),
            "APPLICATION_NOT_FOUND",
            "application-not-found",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request
        );
    }

    @ExceptionHandler(ApplicationConflictException.class)
    ResponseEntity<Object> handleApplicationConflict(
        ApplicationConflictException exception,
        WebRequest request
    ) {
        ConflictDescriptor descriptor = switch (exception.getReason()) {
            case ALREADY_EXISTS -> new ConflictDescriptor(
                "Application already exists",
                "APPLICATION_ALREADY_EXISTS",
                "application-already-exists"
            );
            case INELIGIBLE_JOB_POSTING -> new ConflictDescriptor(
                "Job posting is not eligible",
                "APPLICATION_JOB_POSTING_INELIGIBLE",
                "application-job-posting-ineligible"
            );
            case INVALID_TRANSITION -> new ConflictDescriptor(
                "Application state conflict",
                "APPLICATION_STATE_CONFLICT",
                "application-state-conflict"
            );
            case IDEMPOTENCY_CONFLICT -> new ConflictDescriptor(
                "Idempotency conflict",
                "IDEMPOTENCY_CONFLICT",
                "idempotency-conflict"
            );
        };
        ProblemDetail problem = createProblem(
            HttpStatus.CONFLICT,
            descriptor.title(),
            exception.getMessage(),
            descriptor.code(),
            descriptor.type(),
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.CONFLICT,
            request
        );
    }

    @ExceptionHandler(SubmittedCvNotFoundException.class)
    ResponseEntity<Object> handleSubmittedCvNotFound(
        SubmittedCvNotFoundException exception,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.NOT_FOUND,
            "Submitted CV not found",
            exception.getMessage(),
            "SUBMITTED_CV_NOT_FOUND",
            "submitted-cv-not-found",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request
        );
    }

    @ExceptionHandler(InvalidSubmittedCvException.class)
    ResponseEntity<Object> handleInvalidSubmittedCv(
        InvalidSubmittedCvException exception,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Invalid PDF CV",
            exception.getMessage(),
            "CV_VALIDATION_FAILED",
            "cv-validation-failed",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler(InvalidApplicationRequestException.class)
    ResponseEntity<Object> handleInvalidApplicationRequest(
        InvalidApplicationRequestException exception,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            exception.getMessage(),
            "VALIDATION_FAILED",
            "validation-failed",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @ExceptionHandler(JobPostingNotFoundException.class)
    ResponseEntity<Object> handleJobPostingNotFound(
        JobPostingNotFoundException exception,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.NOT_FOUND,
            "Job posting not found",
            exception.getMessage(),
            "JOB_POSTING_NOT_FOUND",
            "job-posting-not-found",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.NOT_FOUND,
            request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpectedException(
        Exception exception,
        WebRequest request
    ) {
        logger.error("Unhandled exception while processing an API request", exception);

        ProblemDetail problem = createProblem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal server error",
            "An unexpected error occurred",
            "INTERNAL_ERROR",
            "internal-error",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            new HttpHeaders(),
            HttpStatus.INTERNAL_SERVER_ERROR,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        List<ValidationError> errors = exception.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> new ValidationError(
                error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : "request",
                error.getDefaultMessage() == null
                    ? "Invalid value"
                    : error.getDefaultMessage()
            ))
            .distinct()
            .sorted(
                Comparator.comparing(ValidationError::field)
                    .thenComparing(ValidationError::message)
            )
            .toList();

        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Request validation failed",
            "One or more request fields are invalid",
            "VALIDATION_FAILED",
            "validation-failed",
            request
        );
        problem.setProperty("errors", errors);

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
        HttpMessageNotReadableException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Malformed request",
            "The request body is missing or cannot be read",
            "MALFORMED_REQUEST",
            "malformed-request",
            request
        );

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
        TypeMismatchException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Invalid request parameter",
            "A request parameter has an invalid value or format",
            "INVALID_PARAMETER",
            "invalid-parameter",
            request
        );

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(
        ServletRequestBindingException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Missing or invalid request parameter",
            "A required header or request parameter is missing or invalid",
            "INVALID_PARAMETER",
            "invalid-parameter",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            headers,
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
        MaxUploadSizeExceededException exception,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = createProblem(
            HttpStatus.BAD_REQUEST,
            "Invalid PDF CV",
            "The PDF CV must not exceed 5 MiB",
            "CV_VALIDATION_FAILED",
            "cv-validation-failed",
            request
        );

        return handleExceptionInternal(
            exception,
            problem,
            headers,
            HttpStatus.BAD_REQUEST,
            request
        );
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        Exception exception,
        Object body,
        HttpHeaders headers,
        HttpStatusCode status,
        WebRequest request
    ) {
        ProblemDetail problem = body instanceof ProblemDetail problemDetail
            ? problemDetail
            : ProblemDetail.forStatusAndDetail(status, "The request could not be processed");

        if (problem.getProperties() == null
            || !problem.getProperties().containsKey("code")) {
            problem.setProperty("code", "HTTP_" + status.value());
        }
        if (problem.getInstance() == null) {
            problem.setInstance(requestUri(request));
        }

        return super.handleExceptionInternal(exception, problem, headers, status, request);
    }

    private ProblemDetail createProblem(
        HttpStatus status,
        String title,
        String detail,
        String code,
        String type,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("urn:problem:" + type));
        problem.setInstance(requestUri(request));
        problem.setProperty("code", code);
        return problem;
    }

    private URI requestUri(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return URI.create(servletRequest.getRequest().getRequestURI());
        }
        return URI.create("/");
    }

    private record ValidationError(String field, String message) {
    }

    private record ConflictDescriptor(String title, String code, String type) {
    }
}
