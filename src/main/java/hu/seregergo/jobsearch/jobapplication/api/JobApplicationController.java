package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.ApplicationSubmissionReceipt;
import hu.seregergo.jobsearch.jobapplication.application.ApplicationSubmissionService;
import hu.seregergo.jobsearch.jobapplication.application.JobApplicationService;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvDownload;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvFileValidator;
import hu.seregergo.jobsearch.jobapplication.application.SubmittedCvMetadata;
import hu.seregergo.jobsearch.jobapplication.application.StoredOperationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Manage application preparation and progress")
public class JobApplicationController {

    private final JobApplicationService service;
    private final ApplicationSubmissionService submissionService;
    private final SubmittedCvFileValidator cvFileValidator;

    public JobApplicationController(
        JobApplicationService service,
        ApplicationSubmissionService submissionService,
        SubmittedCvFileValidator cvFileValidator
    ) {
        this.service = service;
        this.submissionService = submissionService;
        this.cvFileValidator = cvFileValidator;
    }

    @PostMapping
    @Operation(summary = "Start preparing an application")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Application created in PREPARING stage",
            headers = @Header(
                name = HttpHeaders.LOCATION,
                description = "URI of the created application",
                schema = @Schema(type = "string", format = "uri")
            ),
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobApplicationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Request validation or deserialization failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Job posting not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The posting is ineligible or already has an application",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<JobApplicationResponse> create(
        @Valid @RequestBody CreateApplicationRequest request
    ) {
        JobApplicationResponse response = JobApplicationResponse.from(
            service.create(request.toCommand())
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an application by ID")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobApplicationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "ID is not a valid UUID",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public JobApplicationResponse get(@PathVariable UUID id) {
        return JobApplicationResponse.from(service.get(id));
    }

    @GetMapping
    @Operation(
        summary = "List applications",
        description = "Without a due-date cutoff, results are ordered by most recent update. "
            + "Due work is ordered by due date, update time, then ID."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Applications matching the requested filters",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(implementation = JobApplicationResponse.class)
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "A filter is invalid or dueOnOrBefore is used without active=true",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public List<JobApplicationResponse> list(
        @Valid @ParameterObject ApplicationListQuery query
    ) {
        return service.list(query.active(), query.dueOnOrBefore())
            .stream()
            .map(JobApplicationResponse::from)
            .toList();
    }

    @PutMapping("/{id}/workflow")
    @Operation(
        summary = "Replace an application's workflow fields",
        description = "Use this endpoint to move, close, or reopen an application. "
            + "Initial submission has its own endpoint."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Workflow updated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobApplicationResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Request validation or deserialization failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The requested change conflicts with the current workflow state",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public JobApplicationResponse updateWorkflow(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateApplicationWorkflowRequest request
    ) {
        return JobApplicationResponse.from(
            service.updateWorkflow(id, request.toCommand())
        );
    }

    @PostMapping(
        value = "/{id}/submit",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
        summary = "Submit a prepared application",
        description = "Atomically records the submission, its optional PDF CV, and a "
            + "durable idempotent response."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application submitted",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApplicationSubmissionReceipt.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "A field, idempotency key, or PDF is invalid",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The state conflicts or the idempotency key was reused",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<String> submit(
        @PathVariable UUID id,
        @Parameter(
            description = "Globally unique UUID used to replay this exact request",
            required = true
        )
        @RequestHeader("Idempotency-Key") UUID idempotencyKey,
        @Valid @ModelAttribute SubmitApplicationRequest request
    ) {
        return jsonResponse(
            submissionService.submit(
                id,
                idempotencyKey,
                request.toCommand(cvFileValidator)
            )
        );
    }

    @PostMapping(
        value = "/{id}/record-sent-cv",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
        summary = "Record a previously sent CV",
        description = "Adds the one immutable PDF CV to an active application that "
            + "was already submitted without one."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "CV recorded or the original response replayed",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = SubmittedCvMetadata.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "A field, idempotency key, or PDF is invalid",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "The state conflicts or the idempotency key was reused",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<String> recordSentCv(
        @PathVariable UUID id,
        @Parameter(
            description = "Globally unique UUID used to replay this exact request",
            required = true
        )
        @RequestHeader("Idempotency-Key") UUID idempotencyKey,
        @Valid @ModelAttribute RecordSentCvRequest request
    ) {
        return jsonResponse(
            submissionService.recordSentCv(
                id,
                idempotencyKey,
                request.toCommand(cvFileValidator)
            )
        );
    }

    @GetMapping("/{id}/submitted-cv")
    @Operation(summary = "Download the exact submitted PDF CV")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Stored PDF bytes",
            content = @Content(
                mediaType = MediaType.APPLICATION_PDF_VALUE,
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application or submitted CV not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<byte[]> downloadSubmittedCv(@PathVariable UUID id) {
        SubmittedCvDownload download = submissionService.download(id);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(download.originalFileName(), StandardCharsets.UTF_8)
            .build();
        byte[] bytes = download.bytes();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("X-Content-Type-Options", "nosniff")
            .contentLength(bytes.length)
            .body(bytes);
    }

    private ResponseEntity<String> jsonResponse(StoredOperationResponse response) {
        return ResponseEntity.status(response.status())
            .contentType(MediaType.APPLICATION_JSON)
            .body(response.body());
    }
}
