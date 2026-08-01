package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationActivityType;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationActivityRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationIdempotencyRecordRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.InterviewReportRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvRepository;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import hu.seregergo.jobsearch.jobposting.persistence.JobPostingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationActivityApiIntegrationTests extends PostgreSqlIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-30T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationActivityRepository activityRepository;

    @Autowired
    private InterviewReportRepository reportRepository;

    @Autowired
    private ApplicationIdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private SubmittedCvRepository cvRepository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        activityRepository.deleteAll();
        reportRepository.deleteAll();
        idempotencyRepository.deleteAll();
        cvRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void createsListsRetrievesAndUpdatesActivitiesAndLatestActivityTime()
        throws Exception {
        JobApplication application = saveApplication("Communication timeline");
        Instant referenceTime = Instant.now()
            .minusSeconds(60)
            .truncatedTo(ChronoUnit.MICROS);
        Instant firstOccurredAt = referenceTime.minusSeconds(7_200);

        mockMvc.perform(get("/api/applications/{id}", application.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastActivityAt").doesNotExist());
        mockMvc.perform(get(collectionPath(application)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        MvcResult firstResult = mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    firstOccurredAt,
                    ApplicationActivityType.EMAIL,
                    "  Recruiter confirmed the next round  ",
                    "  Technical interview on Tuesday morning.  "
                )))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.applicationId")
                .value(application.getId().toString()))
            .andExpect(jsonPath("$.occurredAt").value(firstOccurredAt.toString()))
            .andExpect(jsonPath("$.type").value("EMAIL"))
            .andExpect(jsonPath("$.summary")
                .value("Recruiter confirmed the next round"))
            .andExpect(jsonPath("$.details")
                .value("Technical interview on Tuesday morning."))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andReturn();

        JsonNode first = json(firstResult);
        String firstId = first.get("id").stringValue();
        String firstCreatedAt = first.get("createdAt").stringValue();
        String firstLocation = firstResult.getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(get("/api/applications/{id}", application.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lastActivityAt")
                .value(firstOccurredAt.toString()))
            .andExpect(jsonPath("$.activities").doesNotExist());

        Instant secondOccurredAt = referenceTime.minusSeconds(3_600);
        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    secondOccurredAt,
                    ApplicationActivityType.CALL,
                    "Discussed the interview format",
                    null
                )))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.details").doesNotExist());

        mockMvc.perform(get(collectionPath(application)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].summary")
                .value("Discussed the interview format"))
            .andExpect(jsonPath("$[1].id").value(firstId));

        mockMvc.perform(get(firstLocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstId))
            .andExpect(jsonPath("$.type").value("EMAIL"));

        mockMvc.perform(put(firstLocation)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    referenceTime,
                    ApplicationActivityType.FOLLOW_UP,
                    "Sent the requested availability",
                    null
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstId))
            .andExpect(jsonPath("$.occurredAt").value(referenceTime.toString()))
            .andExpect(jsonPath("$.type").value("FOLLOW_UP"))
            .andExpect(jsonPath("$.createdAt").value(firstCreatedAt))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get(collectionPath(application)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(firstId));

        mockMvc.perform(get("/api/applications").param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].lastActivityAt")
                .value(referenceTime.toString()))
            .andExpect(jsonPath("$[0].activities").doesNotExist());
    }

    @Test
    void acceptsClosedApplicationsAndKeepsActivitiesInsideTheirParent()
        throws Exception {
        JobApplication closed = saveApplication("Closed process");
        closed.updateWorkflow(
            ApplicationStage.PREPARING,
            null,
            null,
            null,
            ApplicationOutcome.WITHDRAWN,
            "The role changed",
            BASE_TIME.plusSeconds(60)
        );
        applicationRepository.saveAndFlush(closed);
        JobApplication other = saveApplication("Other process");

        MvcResult result = mockMvc.perform(post(collectionPath(closed))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    Instant.now().minusSeconds(60),
                    ApplicationActivityType.EMAIL,
                    "Recruiter acknowledged the withdrawal",
                    null
                )))
            .andExpect(status().isCreated())
            .andReturn();
        String activityId = json(result).get("id").stringValue();

        mockMvc.perform(get(
                "/api/applications/{applicationId}/activities/{activityId}",
                other.getId(),
                activityId
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code")
                .value("APPLICATION_ACTIVITY_NOT_FOUND"));

        mockMvc.perform(get(
                "/api/applications/{applicationId}/activities/{activityId}",
                closed.getId(),
                UUID.randomUUID()
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type")
                .value("urn:problem:application-activity-not-found"));

        mockMvc.perform(get(
                "/api/applications/{applicationId}/activities",
                UUID.randomUUID()
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void validatesRequestsWithoutChangingTheStoredActivity() throws Exception {
        JobApplication application = saveApplication("Validated activity");

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[*].field", hasItems(
                "occurredAt",
                "type",
                "summary"
            )));

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    Instant.now().plusSeconds(3_600),
                    ApplicationActivityType.EMAIL,
                    "Future message",
                    null
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("occurredAt"));

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    Instant.now().minusSeconds(60),
                    ApplicationActivityType.EMAIL,
                    "Oversized summary",
                    "x".repeat(5_001)
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("details"));

        MvcResult created = mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    Instant.now().minusSeconds(60),
                    ApplicationActivityType.LINKEDIN,
                    "Recruiter sent a message",
                    "Asked about availability."
                )))
            .andExpect(status().isCreated())
            .andReturn();
        String location = created.getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    Instant.now().minusSeconds(30),
                    ApplicationActivityType.CALL,
                    "   ",
                    "Changed details"
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("summary"));

        mockMvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("LINKEDIN"))
            .andExpect(jsonPath("$.summary").value("Recruiter sent a message"))
            .andExpect(jsonPath("$.details").value("Asked about availability."));
    }

    @Test
    void publishesApplicationActivityOperationsAndSchemasInOpenApi()
        throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/activities']"
                    + ".post.responses['201']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/activities']"
                    + ".get.responses['200']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/activities/{activityId}']"
                    + ".get.responses['404']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/activities/{activityId}']"
                    + ".put.responses['200']"
            ).exists())
            .andExpect(jsonPath("$.components.schemas.ApplicationActivityRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.ApplicationActivityResponse")
                .exists())
            .andExpect(jsonPath(
                "$.components.schemas.JobApplicationResponse.properties.lastActivityAt"
            ).exists());
    }

    private String collectionPath(JobApplication application) {
        return "/api/applications/" + application.getId() + "/activities";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String requestJson(
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details
    ) throws Exception {
        return objectMapper.writeValueAsString(new ActivityJson(
            occurredAt,
            type,
            summary,
            details
        ));
    }

    private JobApplication saveApplication(String roleTitle) {
        JobPosting posting = jobPostingRepository.saveAndFlush(JobPosting.create(
            "Example Technologies Kft.",
            roleTitle,
            "Company careers",
            "https://example.com/jobs/" + UUID.randomUUID(),
            null,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2026, 7, 30),
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            null,
            BASE_TIME
        ));
        return applicationRepository.saveAndFlush(JobApplication.create(
            posting,
            "Prepare for the next step",
            LocalDate.now().plusDays(7),
            null,
            BASE_TIME
        ));
    }

    private record ActivityJson(
        Instant occurredAt,
        ApplicationActivityType type,
        String summary,
        String details
    ) {
    }
}
