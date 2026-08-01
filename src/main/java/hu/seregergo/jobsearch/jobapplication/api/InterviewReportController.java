package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.InterviewReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/applications/{applicationId}/interview-reports")
@Tag(
    name = "Interview reports",
    description = "Keep reflections from interview rounds"
)
public class InterviewReportController {

    private final InterviewReportService service;

    public InterviewReportController(InterviewReportService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Add an interview report")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Interview report created",
            headers = @Header(
                name = HttpHeaders.LOCATION,
                description = "URI of the created interview report",
                schema = @Schema(type = "string", format = "uri")
            ),
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InterviewReportResponse.class)
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
        )
    })
    public ResponseEntity<InterviewReportResponse> create(
        @PathVariable UUID applicationId,
        @Valid @RequestBody InterviewReportRequest request
    ) {
        InterviewReportResponse response = InterviewReportResponse.from(
            service.create(applicationId, request.toCommand())
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{reportId}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
        summary = "List an application's interview reports",
        description = "Reports are ordered by interview date and creation time, "
            + "newest first."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Interview reports for the application",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(implementation = InterviewReportResponse.class)
                )
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
    public List<InterviewReportResponse> list(@PathVariable UUID applicationId) {
        return service.list(applicationId)
            .stream()
            .map(InterviewReportResponse::from)
            .toList();
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get one interview report")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Interview report found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InterviewReportResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application or interview report not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public InterviewReportResponse get(
        @PathVariable UUID applicationId,
        @PathVariable UUID reportId
    ) {
        return InterviewReportResponse.from(service.get(applicationId, reportId));
    }

    @PutMapping("/{reportId}")
    @Operation(summary = "Replace an interview report's editable fields")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Interview report updated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = InterviewReportResponse.class)
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
            description = "Application or interview report not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public InterviewReportResponse update(
        @PathVariable UUID applicationId,
        @PathVariable UUID reportId,
        @Valid @RequestBody InterviewReportRequest request
    ) {
        return InterviewReportResponse.from(
            service.update(applicationId, reportId, request.toCommand())
        );
    }
}
