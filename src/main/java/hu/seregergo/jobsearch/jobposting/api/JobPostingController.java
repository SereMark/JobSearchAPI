package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.jobposting.application.JobPostingService;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/job-postings")
@Tag(name = "Job postings", description = "Manage tracked job postings")
public class JobPostingController {

    private final JobPostingService service;

    public JobPostingController(JobPostingService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a job posting")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Job posting created",
            headers = @Header(
                name = HttpHeaders.LOCATION,
                description = "URI of the created job posting",
                schema = @Schema(type = "string", format = "uri")
            ),
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobPostingResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Request validation or deserialization failed",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ResponseEntity<JobPostingResponse> create(
        @Valid @RequestBody CreateJobPostingRequest request
    ) {
        JobPostingResponse response = JobPostingResponse.from(
            service.create(request.toCommand())
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a job posting by ID")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Job posting found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobPostingResponse.class)
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
            description = "Job posting not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public JobPostingResponse get(@PathVariable UUID id) {
        return JobPostingResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a job posting's editable fields")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Job posting updated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JobPostingResponse.class)
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
        )
    })
    public JobPostingResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateJobPostingRequest request
    ) {
        return JobPostingResponse.from(service.update(id, request.toCommand()));
    }

    @GetMapping
    @Operation(summary = "List job postings")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Job postings ordered from newest to oldest",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(implementation = JobPostingSummaryResponse.class)
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "A filter value has an invalid format",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public List<JobPostingSummaryResponse> list(
        @RequestParam(required = false) TargetTrack targetTrack,
        @RequestParam(required = false) JobPostingClassification classification
    ) {
        return service.list(targetTrack, classification)
            .stream()
            .map(JobPostingSummaryResponse::from)
            .toList();
    }

    @GetMapping("/duplicate-candidates")
    @Operation(
        summary = "Find possible duplicate job postings",
        description = "Returns exact source URL or external ID matches as warnings "
            + "without blocking creation or updates"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Possible duplicates ordered from newest to oldest",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(implementation = JobPostingSummaryResponse.class)
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "The source reference is missing or invalid",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public List<JobPostingSummaryResponse> findDuplicateCandidates(
        @Valid @ParameterObject DuplicateJobPostingQuery query
    ) {
        return service.findDuplicateCandidates(
                query.sourceUrl(),
                query.externalId(),
                query.excludeId()
            )
            .stream()
            .map(JobPostingSummaryResponse::from)
            .toList();
    }
}
