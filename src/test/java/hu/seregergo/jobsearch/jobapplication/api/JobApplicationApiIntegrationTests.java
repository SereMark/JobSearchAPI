package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationOutcome;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationIdempotencyRecordRepository;
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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobApplicationApiIntegrationTests extends PostgreSqlIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-30T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private ApplicationIdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private SubmittedCvRepository cvRepository;

    @Autowired
    private JobPostingRepository jobPostingRepository;

    @BeforeEach
    @AfterEach
    void clearDatabase() {
        idempotencyRepository.deleteAll();
        cvRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void createsAndRetrievesApplicationWithItsPostingContext() throws Exception {
        JobPosting posting = savePosting(
            "Java Backend Developer",
            TargetTrack.JAVA,
            JobPostingClassification.B
        );

        MvcResult createResult = mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(posting.getId())))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.jobPostingId").value(posting.getId().toString()))
            .andExpect(jsonPath("$.targetTrack").value("JAVA"))
            .andExpect(jsonPath("$.companyName").value("Example Technologies Kft."))
            .andExpect(jsonPath("$.roleTitle").value("Java Backend Developer"))
            .andExpect(jsonPath("$.stage").value("PREPARING"))
            .andExpect(jsonPath("$.submittedOn").doesNotExist())
            .andExpect(jsonPath("$.nextAction").value("Tailor the CV"))
            .andExpect(jsonPath("$.dueOn").value("2026-08-04"))
            .andExpect(jsonPath("$.note").value("Emphasize recent Spring work"))
            .andExpect(jsonPath("$.active").value(true))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andReturn();

        String location = createResult.getResponse().getHeader(HttpHeaders.LOCATION);
        mockMvc.perform(get(location))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.jobPostingId").value(posting.getId().toString()))
            .andExpect(jsonPath("$.roleTitle").value("Java Backend Developer"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsMissingIneligibleAndAlreadyUsedJobPostings() throws Exception {
        UUID missingPostingId = UUID.randomUUID();
        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(missingPostingId)))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("JOB_POSTING_NOT_FOUND"));

        JobPosting skipped = savePosting(
            "Skipped role",
            TargetTrack.JAVA,
            JobPostingClassification.C
        );
        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(skipped.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type")
                .value("urn:problem:application-job-posting-ineligible"))
            .andExpect(jsonPath("$.code")
                .value("APPLICATION_JOB_POSTING_INELIGIBLE"));

        JobPosting eligible = savePosting(
            "Eligible role",
            TargetTrack.DOTNET,
            JobPostingClassification.A
        );
        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(eligible.getId())))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestJson(eligible.getId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type")
                .value("urn:problem:application-already-exists"))
            .andExpect(jsonPath("$.code").value("APPLICATION_ALREADY_EXISTS"));
    }

    @Test
    void listsAllOpenClosedAndInclusiveDueWorkInDefinedOrder() throws Exception {
        JobApplication earliestDue = saveApplication(
            "Earliest due",
            LocalDate.of(2026, 8, 2),
            BASE_TIME
        );
        JobApplication cutoffDue = saveApplication(
            "Cutoff due",
            LocalDate.of(2026, 8, 3),
            BASE_TIME.plusSeconds(60)
        );
        JobApplication futureDue = saveApplication(
            "Future due",
            LocalDate.of(2026, 8, 4),
            BASE_TIME.plusSeconds(120)
        );
        JobApplication closed = saveApplication(
            "Closed application",
            LocalDate.of(2026, 8, 1),
            BASE_TIME.plusSeconds(180)
        );
        closed.updateWorkflow(
            ApplicationStage.PREPARING,
            null,
            null,
            null,
            ApplicationOutcome.ROLE_CANCELLED,
            "The company cancelled the role",
            BASE_TIME.plusSeconds(300)
        );
        applicationRepository.saveAndFlush(closed);

        mockMvc.perform(get("/api/applications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0].id").value(closed.getId().toString()))
            .andExpect(jsonPath("$[1].id").value(futureDue.getId().toString()))
            .andExpect(jsonPath("$[2].id").value(cutoffDue.getId().toString()))
            .andExpect(jsonPath("$[3].id").value(earliestDue.getId().toString()));

        mockMvc.perform(get("/api/applications").param("active", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].id").value(futureDue.getId().toString()))
            .andExpect(jsonPath("$[2].id").value(earliestDue.getId().toString()));

        mockMvc.perform(get("/api/applications").param("active", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].id").value(closed.getId().toString()))
            .andExpect(jsonPath("$[0].active").value(false));

        mockMvc.perform(get("/api/applications")
                .param("active", "true")
                .param("dueOnOrBefore", "2026-08-03"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(earliestDue.getId().toString()))
            .andExpect(jsonPath("$[1].id").value(cutoffDue.getId().toString()));

        mockMvc.perform(get("/api/applications")
                .param("dueOnOrBefore", "2026-08-03"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("active"));
    }

    @Test
    void supportsSubmissionNonLinearProgressClosureReopeningAndSigning()
        throws Exception {
        JobApplication application = saveApplication(
            "Workflow role",
            LocalDate.of(2026, 8, 3),
            BASE_TIME
        );

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "stage": "PREPARING",
                      "stageLabel": "  CV tailoring  ",
                      "nextAction": "  Finish the cover letter  ",
                      "dueOn": "2026-08-03",
                      "outcome": null,
                      "note": "  Waiting for one reference  "
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stageLabel").value("CV tailoring"))
            .andExpect(jsonPath("$.nextAction").value("Finish the cover letter"))
            .andExpect(jsonPath("$.note").value("Waiting for one reference"));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("FINAL", "Prepare examples")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.type")
                .value("urn:problem:application-state-conflict"))
            .andExpect(jsonPath("$.code").value("APPLICATION_STATE_CONFLICT"));

        LocalDate today = LocalDate.now();
        mockMvc.perform(submitRequest(application.getId(), UUID.randomUUID(), today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("SUBMITTED"))
            .andExpect(jsonPath("$.submittedOn").value(today.toString()))
            .andExpect(jsonPath("$.nextAction").value("Check for a response"))
            .andExpect(jsonPath("$.submittedCv").doesNotExist());

        mockMvc.perform(submitRequest(application.getId(), UUID.randomUUID(), today))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("FINAL", "Prepare architecture examples")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("FINAL"))
            .andExpect(jsonPath("$.submittedOn").value(today.toString()));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson("FINAL", "REJECTED", "Role filled")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("REJECTED"))
            .andExpect(jsonPath("$.nextAction").doesNotExist())
            .andExpect(jsonPath("$.dueOn").doesNotExist())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson(
                    "RECRUITER_SCREEN",
                    "Confirm the reopened interview slot"
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("RECRUITER_SCREEN"))
            .andExpect(jsonPath("$.submittedOn").value(today.toString()))
            .andExpect(jsonPath("$.outcome").doesNotExist())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("OFFER", "Review the contract")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("OFFER"))
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson("OFFER", "SIGNED", "Contract signed")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("SIGNED"))
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("OFFER", "Undo signing")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_STATE_CONFLICT"));
    }

    @Test
    void preparingClosureMustBeReopenedBeforeSubmission() throws Exception {
        JobApplication application = saveApplication(
            "Reconsidered role",
            LocalDate.of(2026, 8, 3),
            BASE_TIME
        );

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson(
                    "PREPARING",
                    "WITHDRAWN",
                    "Decided not to apply"
                )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        LocalDate today = LocalDate.now();
        UUID idempotencyKey = UUID.randomUUID();
        mockMvc.perform(submitRequest(application.getId(), idempotencyKey, today))
            .andExpect(status().isConflict());

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("PREPARING", "Finish the application")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(submitRequest(application.getId(), idempotencyKey, today))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stage").value("SUBMITTED"));
    }

    @Test
    void returnsProblemDetailsForInvalidRequestsAndMissingApplications()
        throws Exception {
        mockMvc.perform(post("/api/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[*].field", hasItems(
                "jobPostingId",
                "nextAction",
                "dueOn"
            )));

        JobApplication application = saveApplication(
            "Validation role",
            LocalDate.of(2026, 8, 3),
            BASE_TIME
        );
        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson("FINAL", "SIGNED", "Signed too early")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("outcome"));

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        mockMvc.perform(submitRequest(application.getId(), UUID.randomUUID(), tomorrow))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("submittedOn"));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("UNKNOWN_STAGE", "Wait")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        UUID missingId = UUID.randomUUID();
        mockMvc.perform(get("/api/applications/{id}", missingId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("urn:problem:application-not-found"))
            .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));

        mockMvc.perform(put("/api/applications/{id}/workflow", missingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(activeWorkflowJson("PREPARING", "Finish the CV")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    void publishesApplicationOperationsAndSchemasInOpenApi() throws Exception {
        MvcResult result = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paths['/api/applications'].post.responses['201']")
                .exists())
            .andExpect(jsonPath("$.paths['/api/applications'].post.responses['409']")
                .exists())
            .andExpect(jsonPath("$.paths['/api/applications'].get.parameters[*].name",
                hasItems("active", "dueOnOrBefore")))
            .andExpect(jsonPath("$.paths['/api/applications/{id}'].get.responses['404']")
                .exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/workflow'].put.responses['409']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/submit'].post.responses['200']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/submit'].post.requestBody"
                    + ".content['multipart/form-data']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/submit'].post.parameters[*].name",
                hasItems("id", "Idempotency-Key")
            ))
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/record-sent-cv']"
                    + ".post.responses['409']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/record-sent-cv']"
                    + ".post.parameters[*].name",
                hasItems("id", "Idempotency-Key")
            ))
            .andExpect(jsonPath(
                "$.paths['/api/applications/{id}/submitted-cv']"
                    + ".get.responses['200'].content['application/pdf']"
            ).exists())
            .andExpect(jsonPath("$.components.schemas.CreateApplicationRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.UpdateApplicationWorkflowRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.SubmitApplicationRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.RecordSentCvRequest")
                .exists())
            .andExpect(jsonPath(
                "$.components.schemas.SubmitApplicationRequest.properties.cv.format"
            ).value("binary"))
            .andExpect(jsonPath(
                "$.components.schemas.RecordSentCvRequest.properties.cv.format"
            ).value("binary"))
            .andExpect(jsonPath("$.components.schemas.JobApplicationResponse")
                .exists())
            .andReturn();

        String apiDocument = result.getResponse().getContentAsString();
        assertFalse(apiDocument.contains("ValidApplicationWorkflow"));
        assertFalse(apiDocument.contains("ValidApplicationListQuery"));
    }

    private JobApplication saveApplication(
        String roleTitle,
        LocalDate dueOn,
        Instant createdAt
    ) {
        JobPosting posting = savePosting(
            roleTitle,
            TargetTrack.JAVA,
            JobPostingClassification.A
        );
        return applicationRepository.saveAndFlush(JobApplication.create(
            posting,
            "Next action for " + roleTitle,
            dueOn,
            null,
            createdAt
        ));
    }

    private JobPosting savePosting(
        String roleTitle,
        TargetTrack targetTrack,
        JobPostingClassification classification
    ) {
        return jobPostingRepository.saveAndFlush(JobPosting.create(
            "Example Technologies Kft.",
            roleTitle,
            "Company careers",
            "https://example.com/jobs/" + UUID.randomUUID(),
            null,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2026, 7, 30),
            targetTrack,
            classification,
            classification == JobPostingClassification.C
                ? "Not a suitable role"
                : null,
            null,
            BASE_TIME
        ));
    }

    private String createRequestJson(UUID postingId) {
        return """
            {
              "jobPostingId": "%s",
              "nextAction": "  Tailor the CV  ",
              "dueOn": "2026-08-04",
              "note": "  Emphasize recent Spring work  "
            }
            """.formatted(postingId);
    }

    private String activeWorkflowJson(String stage, String nextAction) {
        return """
            {
              "stage": "%s",
              "stageLabel": null,
              "nextAction": "%s",
              "dueOn": "2026-08-08",
              "outcome": null,
              "note": null
            }
            """.formatted(stage, nextAction);
    }

    private String closedWorkflowJson(String stage, String outcome, String note) {
        return """
            {
              "stage": "%s",
              "stageLabel": null,
              "nextAction": null,
              "dueOn": null,
              "outcome": "%s",
              "note": "%s"
            }
            """.formatted(stage, outcome, note);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
        submitRequest(UUID applicationId, UUID idempotencyKey, LocalDate submittedOn) {
        return multipart("/api/applications/{id}/submit", applicationId)
            .header("Idempotency-Key", idempotencyKey)
            .param("submittedOn", submittedOn.toString())
            .param("nextAction", "Check for a response")
            .param("dueOn", submittedOn.plusDays(7).toString());
    }
}
