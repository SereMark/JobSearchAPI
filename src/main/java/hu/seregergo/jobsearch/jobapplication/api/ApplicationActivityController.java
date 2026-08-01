package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.jobapplication.application.ApplicationActivityService;
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
@RequestMapping("/api/applications/{applicationId}/activities")
@Tag(
    name = "Application activities",
    description = "Keep a chronological record of communication and follow-up"
)
public class ApplicationActivityController {

    private final ApplicationActivityService service;

    public ApplicationActivityController(ApplicationActivityService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Add an application activity")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Application activity created",
            headers = @Header(
                name = HttpHeaders.LOCATION,
                description = "URI of the created activity",
                schema = @Schema(type = "string", format = "uri")
            ),
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApplicationActivityResponse.class)
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
    public ResponseEntity<ApplicationActivityResponse> create(
        @PathVariable UUID applicationId,
        @Valid @RequestBody ApplicationActivityRequest request
    ) {
        ApplicationActivityResponse response = ApplicationActivityResponse.from(
            service.create(applicationId, request.toCommand())
        );
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{activityId}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    @Operation(
        summary = "List an application's activities",
        description = "Activities are ordered by occurrence and creation time, "
            + "newest first."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Activities for the application",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                array = @ArraySchema(
                    schema = @Schema(implementation = ApplicationActivityResponse.class)
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
    public List<ApplicationActivityResponse> list(
        @PathVariable UUID applicationId
    ) {
        return service.list(applicationId)
            .stream()
            .map(ApplicationActivityResponse::from)
            .toList();
    }

    @GetMapping("/{activityId}")
    @Operation(summary = "Get one application activity")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application activity found",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApplicationActivityResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Application or activity not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ApplicationActivityResponse get(
        @PathVariable UUID applicationId,
        @PathVariable UUID activityId
    ) {
        return ApplicationActivityResponse.from(
            service.get(applicationId, activityId)
        );
    }

    @PutMapping("/{activityId}")
    @Operation(summary = "Replace an application activity's editable fields")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Application activity updated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ApplicationActivityResponse.class)
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
            description = "Application or activity not found",
            content = @Content(
                mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                schema = @Schema(implementation = ProblemDetail.class)
            )
        )
    })
    public ApplicationActivityResponse update(
        @PathVariable UUID applicationId,
        @PathVariable UUID activityId,
        @Valid @RequestBody ApplicationActivityRequest request
    ) {
        return ApplicationActivityResponse.from(
            service.update(applicationId, activityId, request.toCommand())
        );
    }
}
