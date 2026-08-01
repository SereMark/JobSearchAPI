package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.persistence.ApplicationIdempotencyRecordRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.JobApplicationRepository;
import hu.seregergo.jobsearch.jobapplication.persistence.SubmittedCvRepository;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
import hu.seregergo.jobsearch.jobposting.domain.TargetTrack;
import hu.seregergo.jobsearch.jobposting.domain.WorkMode;
import hu.seregergo.jobsearch.jobposting.persistence.JobPostingRepository;
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

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobPostingApiIntegrationTests extends PostgreSqlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JobPostingRepository repository;

    @Autowired
    private JobApplicationRepository applicationRepository;

    @Autowired
    private ApplicationIdempotencyRecordRepository idempotencyRepository;

    @Autowired
    private SubmittedCvRepository cvRepository;

    @BeforeEach
    void clearDatabase() {
        idempotencyRepository.deleteAll();
        cvRepository.deleteAll();
        applicationRepository.deleteAll();
        repository.deleteAll();
    }

    @Test
    void createsAndRetrievesCompleteJobPosting() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/job-postings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
            .andExpect(status().isCreated())
            .andExpect(header().exists(HttpHeaders.LOCATION))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.companyName").value("Example Technologies Kft."))
            .andExpect(jsonPath("$.roleTitle").value("Java Backend Developer"))
            .andExpect(jsonPath("$.workMode").value("HYBRID"))
            .andExpect(jsonPath("$.targetTrack").value("JAVA"))
            .andExpect(jsonPath("$.classification").value("A"))
            .andExpect(jsonPath("$.descriptionSnapshot")
                .value("Build and maintain Java backend services."))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andExpect(jsonPath("$.updatedAt").isNotEmpty())
            .andReturn();

        JsonNode responseBody = objectMapper.readTree(
            createResult.getResponse().getContentAsString()
        );
        UUID id = UUID.fromString(responseBody.get("id").stringValue());
        String createdAt = responseBody.get("createdAt").stringValue();
        String location = createResult.getResponse().getHeader(HttpHeaders.LOCATION);
        assertTrue(location.endsWith("/api/job-postings/" + id));
        assertEquals(createdAt, responseBody.get("updatedAt").stringValue());

        mockMvc.perform(get("/api/job-postings/{id}", id))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.sourceUrl")
                .value("https://careers.example.com/jobs/123"))
            .andExpect(jsonPath("$.externalId").value("JOB-123"))
            .andExpect(jsonPath("$.foundOn").value("2020-01-15"))
            .andExpect(jsonPath("$.targetTrack").value("JAVA"))
            .andExpect(jsonPath("$.descriptionSnapshot")
                .value("Build and maintain Java backend services."))
            .andExpect(jsonPath("$.createdAt").value(createdAt));

        mockMvc.perform(get(URI.create(location)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void replacesEditableFieldsAndPreservesCreationMetadata() throws Exception {
        Instant createdAt = Instant.parse("2026-07-30T10:00:00Z");
        JobPosting original = repository.saveAndFlush(jobPosting(
            "Original role",
            "https://example.com/jobs/original",
            null,
            TargetTrack.JAVA,
            JobPostingClassification.A,
            "Original description",
            createdAt
        ));

        MvcResult updateResult = mockMvc.perform(put(
                "/api/job-postings/{id}",
                original.getId()
            )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "companyName": "Updated Technologies Kft.",
                      "roleTitle": ".NET Backend Developer",
                      "source": "Recruiter",
                      "sourceUrl": null,
                      "externalId": "DOTNET-456",
                      "location": "Budapest",
                      "workMode": "REMOTE",
                      "foundOn": "2026-07-30",
                      "targetTrack": "DOTNET",
                      "classification": "B",
                      "reviewNote": "One clarification remains",
                      "descriptionSnapshot": null
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(original.getId().toString()))
            .andExpect(jsonPath("$.companyName").value("Updated Technologies Kft."))
            .andExpect(jsonPath("$.roleTitle").value(".NET Backend Developer"))
            .andExpect(jsonPath("$.sourceUrl").doesNotExist())
            .andExpect(jsonPath("$.externalId").value("DOTNET-456"))
            .andExpect(jsonPath("$.targetTrack").value("DOTNET"))
            .andExpect(jsonPath("$.classification").value("B"))
            .andExpect(jsonPath("$.descriptionSnapshot").doesNotExist())
            .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
            .andReturn();

        JsonNode responseBody = objectMapper.readTree(
            updateResult.getResponse().getContentAsString()
        );
        Instant updatedAt = Instant.parse(responseBody.get("updatedAt").stringValue());
        assertTrue(updatedAt.isAfter(createdAt));

        JobPosting persisted = repository.findById(original.getId()).orElseThrow();
        assertEquals(TargetTrack.DOTNET, persisted.getTargetTrack());
        assertEquals(updatedAt, persisted.getUpdatedAt());
    }

    @Test
    void listsSummariesNewestFirstAndFiltersWithAndSemantics() throws Exception {
        JobPosting javaAOlder = jobPosting(
            "Java A older",
            "https://example.com/jobs/java-a-older",
            null,
            TargetTrack.JAVA,
            JobPostingClassification.A,
            "Stored advert text",
            Instant.parse("2026-07-28T10:00:00Z")
        );
        JobPosting javaB = jobPosting(
            "Java B",
            "https://example.com/jobs/java-b",
            null,
            TargetTrack.JAVA,
            JobPostingClassification.B,
            null,
            Instant.parse("2026-07-29T10:00:00Z")
        );
        JobPosting dotnetA = jobPosting(
            ".NET A",
            "https://example.com/jobs/dotnet-a",
            null,
            TargetTrack.DOTNET,
            JobPostingClassification.A,
            null,
            Instant.parse("2026-07-30T10:00:00Z")
        );
        repository.saveAllAndFlush(List.of(javaAOlder, javaB, dotnetA));

        mockMvc.perform(get("/api/job-postings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].roleTitle").value(".NET A"))
            .andExpect(jsonPath("$[1].roleTitle").value("Java B"))
            .andExpect(jsonPath("$[2].roleTitle").value("Java A older"))
            .andExpect(jsonPath("$[2].hasDescriptionSnapshot").value(true))
            .andExpect(jsonPath("$[2].descriptionSnapshot").doesNotExist());

        mockMvc.perform(get("/api/job-postings").param("targetTrack", "JAVA"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].roleTitle").value("Java B"))
            .andExpect(jsonPath("$[1].roleTitle").value("Java A older"));

        mockMvc.perform(get("/api/job-postings").param("classification", "A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].roleTitle").value(".NET A"))
            .andExpect(jsonPath("$[1].roleTitle").value("Java A older"));

        mockMvc.perform(get("/api/job-postings")
                .param("targetTrack", "JAVA")
                .param("classification", "A"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].roleTitle").value("Java A older"));
    }

    @Test
    void findsDuplicateCandidatesWithoutBlockingLegitimateRecords() throws Exception {
        JobPosting urlMatch = repository.saveAndFlush(jobPosting(
            "Original URL match",
            "https://example.com/jobs/shared",
            "ORIGINAL-1",
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            Instant.parse("2026-07-28T10:00:00Z")
        ));
        JobPosting externalIdMatch = repository.saveAndFlush(jobPosting(
            "External ID match",
            "https://example.com/jobs/different",
            "SHARED-ID",
            TargetTrack.JAVA,
            JobPostingClassification.B,
            null,
            Instant.parse("2026-07-29T10:00:00Z")
        ));
        repository.saveAndFlush(jobPosting(
            "Unrelated role",
            "https://example.com/jobs/unrelated",
            "UNRELATED",
            TargetTrack.DOTNET,
            JobPostingClassification.A,
            null,
            Instant.parse("2026-07-30T10:00:00Z")
        ));

        mockMvc.perform(get("/api/job-postings/duplicate-candidates")
                .param("sourceUrl", "https://example.com/jobs/shared")
                .param("externalId", "SHARED-ID"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(externalIdMatch.getId().toString()))
            .andExpect(jsonPath("$[1].id").value(urlMatch.getId().toString()));

        mockMvc.perform(get("/api/job-postings/duplicate-candidates")
                .param("sourceUrl", "https://example.com/jobs/shared")
                .param("excludeId", urlMatch.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/job-postings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson().replace(
                    "https://careers.example.com/jobs/123",
                    "https://example.com/jobs/shared"
                )))
            .andExpect(status().isCreated());

        assertEquals(4, repository.count());
    }

    @Test
    void returnsProblemDetailsForInvalidCreateAndUpdateRequests() throws Exception {
        String invalidRequest = """
            {
              "companyName": " ",
              "roleTitle": "",
              "source": "",
              "sourceUrl": null,
              "externalId": null,
              "location": null,
              "workMode": null,
              "foundOn": "2999-01-01",
              "targetTrack": null,
              "classification": "C",
              "reviewNote": null,
              "descriptionSnapshot": null
            }
            """;

        mockMvc.perform(post("/api/job-postings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:validation-failed"))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.instance").value("/api/job-postings"))
            .andExpect(jsonPath("$.errors[*].field", hasItems(
                "companyName",
                "roleTitle",
                "source",
                "sourceUrl",
                "workMode",
                "foundOn",
                "targetTrack",
                "reviewNote"
            )));

        JobPosting existing = repository.saveAndFlush(jobPosting(
            "Original role",
            "https://example.com/jobs/original",
            null,
            TargetTrack.JAVA,
            JobPostingClassification.A,
            null,
            Instant.parse("2026-07-30T10:00:00Z")
        ));

        mockMvc.perform(put("/api/job-postings/{id}", existing.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.instance")
                .value("/api/job-postings/" + existing.getId()));

        assertEquals(1, repository.count());
        assertEquals("Original role", repository.findById(existing.getId())
            .orElseThrow()
            .getRoleTitle());
    }

    @Test
    void returnsProblemDetailsForInvalidDuplicateQuery() throws Exception {
        mockMvc.perform(get("/api/job-postings/duplicate-candidates"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:validation-failed"))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value("sourceUrl"));

        mockMvc.perform(get("/api/job-postings/duplicate-candidates")
                .param("sourceUrl", "ftp://example.com/job"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void returnsProblemDetailsForMalformedRequestsAndParameters() throws Exception {
        mockMvc.perform(post("/api/job-postings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyName\":"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:malformed-request"))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        mockMvc.perform(get("/api/job-postings").param("targetTrack", "RUBY"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:invalid-parameter"))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void returnsProblemDetailsWhenJobPostingDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/job-postings/{id}", missingId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:job-posting-not-found"))
            .andExpect(jsonPath("$.code").value("JOB_POSTING_NOT_FOUND"));

        mockMvc.perform(put("/api/job-postings/{id}", missingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.instance")
                .value("/api/job-postings/" + missingId));
    }

    @Test
    void returnsProblemDetailsForMalformedId() throws Exception {
        mockMvc.perform(get("/api/job-postings/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:invalid-parameter"))
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test
    void publishesOpenApiDocumentAndSwaggerUi() throws Exception {
        MvcResult apiDocumentResult = mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.info.title").value("Job Search API"))
            .andExpect(jsonPath("$.info.version").value("0.1.0"))
            .andExpect(jsonPath("$.info.description")
                .value("Local API for evaluating and tracking job opportunities"))
            .andExpect(jsonPath("$.paths['/api/job-postings'].post.responses['201']")
                .exists())
            .andExpect(jsonPath("$.paths['/api/job-postings'].get.parameters[*].name",
                hasItems("targetTrack", "classification")))
            .andExpect(jsonPath("$.paths['/api/job-postings'].get.responses['400']")
                .exists())
            .andExpect(jsonPath(
                "$.paths['/api/job-postings/{id}'].put.responses['200']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/job-postings/{id}'].put.responses['404']"
            ).exists())
            .andExpect(jsonPath(
                "$.paths['/api/job-postings/duplicate-candidates'].get.responses['400']"
            ).exists())
            .andExpect(jsonPath("$.components.schemas.CreateJobPostingRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.UpdateJobPostingRequest")
                .exists())
            .andExpect(jsonPath("$.components.schemas.JobPostingSummaryResponse")
                .exists())
            .andExpect(jsonPath(
                "$.components.schemas.CreateJobPostingRequest.properties.targetTrack.example"
            ).value("JAVA"))
            .andReturn();

        String apiDocument = apiDocumentResult.getResponse().getContentAsString();
        assertFalse(apiDocument.contains("ValidJobPostingRequest"));

        mockMvc.perform(get("/swagger-ui.html"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string(HttpHeaders.LOCATION, "/swagger-ui/index.html"));
    }

    private String validRequestJson() {
        return """
            {
              "companyName": "Example Technologies Kft.",
              "roleTitle": "Java Backend Developer",
              "source": "Company careers",
              "sourceUrl": "https://careers.example.com/jobs/123",
              "externalId": "JOB-123",
              "location": "Budapest",
              "workMode": "HYBRID",
              "foundOn": "2020-01-15",
              "targetTrack": "JAVA",
              "classification": "A",
              "reviewNote": null,
              "descriptionSnapshot": "Build and maintain Java backend services."
            }
            """;
    }

    private JobPosting jobPosting(
        String roleTitle,
        String sourceUrl,
        String externalId,
        TargetTrack targetTrack,
        JobPostingClassification classification,
        String descriptionSnapshot,
        Instant createdAt
    ) {
        return JobPosting.create(
            "Example Technologies Kft.",
            roleTitle,
            "Company careers",
            sourceUrl,
            externalId,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2020, 1, 15),
            targetTrack,
            classification,
            classification == JobPostingClassification.C
                ? "Not a suitable role"
                : null,
            descriptionSnapshot,
            createdAt
        );
    }
}
