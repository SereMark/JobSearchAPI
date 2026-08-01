package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
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
class InterviewReportApiIntegrationTests extends PostgreSqlIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-30T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        reportRepository.deleteAll();
        idempotencyRepository.deleteAll();
        cvRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void createsListsRetrievesAndUpdatesInterviewReports() throws Exception {
        JobApplication application = saveApplication("Interview timeline");
        LocalDate today = LocalDate.now();

        MvcResult firstResult = mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    today.minusDays(2),
                    "  Recruiter screen  ",
                    "  Friendly conversation about the role and expectations.  "
                )))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.applicationId").value(application.getId().toString()))
            .andExpect(jsonPath("$.interviewedOn").value(today.minusDays(2).toString()))
            .andExpect(jsonPath("$.roundLabel").value("Recruiter screen"))
            .andExpect(jsonPath("$.report").value(
                "Friendly conversation about the role and expectations."
            ))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andReturn();

        JsonNode first = json(firstResult);
        String firstId = first.get("id").stringValue();
        String firstCreatedAt = first.get("createdAt").stringValue();
        String firstLocation = firstResult.getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    today.minusDays(1),
                    "Technical interview - round 1",
                    "Strong architecture discussion; review isolation levels."
                )))
            .andExpect(status().isCreated());

        mockMvc.perform(get(collectionPath(application)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].roundLabel")
                .value("Technical interview - round 1"))
            .andExpect(jsonPath("$[1].id").value(firstId));

        mockMvc.perform(get(firstLocation))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstId))
            .andExpect(jsonPath("$.roundLabel").value("Recruiter screen"));

        mockMvc.perform(put(firstLocation)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    today,
                    "Hiring manager follow-up",
                    "Clarified ownership, team boundaries, and expectations."
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstId))
            .andExpect(jsonPath("$.interviewedOn").value(today.toString()))
            .andExpect(jsonPath("$.roundLabel").value("Hiring manager follow-up"))
            .andExpect(jsonPath("$.createdAt").value(firstCreatedAt))
            .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        mockMvc.perform(get(collectionPath(application)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(firstId));

        mockMvc.perform(get("/api/applications/{id}", application.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.interviewReports").doesNotExist());
    }

    @Test
    void acceptsClosedApplicationsAndKeepsReportsInsideTheirParent() throws Exception {
        JobApplication closed = saveApplication("Closed process");
        closed.updateWorkflow(
            ApplicationStage.PREPARING,
            null,
            null,
            null,
            ApplicationOutcome.WITHDRAWN,
            "The role changed after the interview",
            BASE_TIME.plusSeconds(60)
        );
        applicationRepository.saveAndFlush(closed);
        JobApplication other = saveApplication("Other process");

        MvcResult result = mockMvc.perform(post(collectionPath(closed))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    LocalDate.now(),
                    "Final reflection",
                    "Useful lessons despite the closed process."
                )))
            .andExpect(status().isCreated())
            .andReturn();
        String reportId = json(result).get("id").stringValue();

        mockMvc.perform(get(
                "/api/applications/{applicationId}/interview-reports/{reportId}",
                other.getId(),
                reportId
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("INTERVIEW_REPORT_NOT_FOUND"));

        mockMvc.perform(get(
                "/api/applications/{applicationId}/interview-reports/{reportId}",
                closed.getId(),
                UUID.randomUUID()
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type")
                .value("urn:problem:interview-report-not-found"));

        mockMvc.perform(get(
                "/api/applications/{applicationId}/interview-reports",
                UUID.randomUUID()
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void validatesRequestsWithoutChangingTheStoredReport() throws Exception {
        JobApplication application = saveApplication("Validated notes");

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[*].field", hasItems(
                "interviewedOn",
                "roundLabel",
                "report"
            )));

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    LocalDate.now().plusDays(1),
                    "Future interview",
                    "This report cannot exist yet."
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("interviewedOn"));

        mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    LocalDate.now(),
                    "Oversized report",
                    "x".repeat(20_001)
                )))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("report"));

        MvcResult created = mockMvc.perform(post(collectionPath(application))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(
                    LocalDate.now(),
                    "Recruiter screen",
                    "Original reflection."
                )))
            .andExpect(status().isCreated())
            .andReturn();
        String location = created.getResponse().getHeader(HttpHeaders.LOCATION);

        mockMvc.perform(put(location)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson(LocalDate.now(), "   ", "Changed reflection.")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("roundLabel"));

        mockMvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roundLabel").value("Recruiter screen"))
            .andExpect(jsonPath("$.report").value("Original reflection."));
    }

    @Test
    void publishesInterviewReportOperationsAndSchemasInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/interview-reports']"
                    + ".post.responses['201']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/interview-reports']"
                    + ".get.responses['200']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/interview-reports/{reportId}']"
                    + ".get.responses['404']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{applicationId}/interview-reports/{reportId}']"
                    + ".put.responses['200']"
            ).exists())
            .andExpect(jsonPath("$.components.schemas.InterviewReportRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.InterviewReportResponse")
                .exists());
    }

    private String collectionPath(JobApplication application) {
        return "/api/applications/" + application.getId() + "/interview-reports";
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private String requestJson(
        LocalDate interviewedOn,
        String roundLabel,
        String report
    ) throws Exception {
        return objectMapper.writeValueAsString(new ReportJson(
            interviewedOn,
            roundLabel,
            report
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

    private record ReportJson(
        LocalDate interviewedOn,
        String roundLabel,
        String report
    ) {
    }
}
