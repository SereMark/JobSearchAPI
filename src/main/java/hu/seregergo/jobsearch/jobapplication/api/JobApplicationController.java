package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.JobApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications", description = "Manage application preparation and progress")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
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

    @PostMapping("/{id}/submit")
    @Operation(
        summary = "Mark a prepared application as submitted",
        description = "Moves an active PREPARING application to SUBMITTED and records "
            + "its submission date."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application submitted",
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
            description = "The application is not active and PREPARING",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public JobApplicationResponse submit(
        @PathVariable UUID id,
        @Valid @RequestBody SubmitApplicationRequest request
    ) {
        return JobApplicationResponse.from(service.submit(id, request.toCommand()));
    }
}
