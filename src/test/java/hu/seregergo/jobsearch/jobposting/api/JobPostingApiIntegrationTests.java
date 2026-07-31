package hu.seregergo.jobsearch.jobposting.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobposting.domain.JobPosting;
import hu.seregergo.jobsearch.jobposting.domain.JobPostingClassification;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void createsAndRetrievesJobPosting() throws Exception {
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
            .andExpect(jsonPath("$.classification").value("A"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty())
            .andReturn();

        JsonNode responseBody = objectMapper.readTree(
            createResult.getResponse().getContentAsString()
        );
        UUID id = UUID.fromString(responseBody.get("id").stringValue());
        String createdAt = responseBody.get("createdAt").stringValue();
        String location = createResult.getResponse().getHeader(HttpHeaders.LOCATION);
        assertTrue(location.endsWith("/api/job-postings/" + id));

        mockMvc.perform(get("/api/job-postings/{id}", id))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.sourceUrl")
                .value("https://careers.example.com/jobs/123"))
            .andExpect(jsonPath("$.externalId").value("JOB-123"))
            .andExpect(jsonPath("$.foundOn").value("2020-01-15"))
            .andExpect(jsonPath("$.createdAt").value(createdAt));

        mockMvc.perform(get(URI.create(location)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void listsNewestJobPostingsFirst() throws Exception {
        JobPosting older = jobPosting(
            "Older role",
            "https://example.com/jobs/older",
            Instant.parse("2026-07-29T10:00:00Z")
        );
        JobPosting newer = jobPosting(
            "Newer role",
            "https://example.com/jobs/newer",
            Instant.parse("2026-07-30T10:00:00Z")
        );
        repository.saveAllAndFlush(List.of(older, newer));

        mockMvc.perform(get("/api/job-postings"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].roleTitle").value("Newer role"))
            .andExpect(jsonPath("$[1].roleTitle").value("Older role"));
    }

    @Test
    void returnsProblemDetailsForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/job-postings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "companyName": " ",
                      "roleTitle": "",
                      "source": "",
                      "sourceUrl": null,
                      "externalId": null,
                      "location": null,
                      "workMode": null,
                      "foundOn": "2999-01-01",
                      "classification": "C",
                      "reviewNote": null
                    }
                    """))
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
                "reviewNote"
            )));

        assertEquals(0, repository.count());
    }

    @Test
    void returnsProblemDetailsForMalformedRequest() throws Exception {
        mockMvc.perform(post("/api/job-postings")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"companyName\":"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:malformed-request"))
            .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
            .andExpect(jsonPath("$.instance").value("/api/job-postings"));
    }

    @Test
    void returnsProblemDetailsWhenJobPostingDoesNotExist() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/api/job-postings/{id}", missingId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.type").value("urn:problem:job-posting-not-found"))
            .andExpect(jsonPath("$.code").value("JOB_POSTING_NOT_FOUND"))
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
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.info.title").value("Job Search API"))
            .andExpect(jsonPath("$.info.version").value("0.1.0"))
            .andExpect(jsonPath("$.info.description")
                .value("Local API for tracking job postings"))
            .andExpect(jsonPath("$.paths['/api/job-postings']").exists())
            .andExpect(jsonPath("$.paths['/api/job-postings'].post.responses['201']")
                .exists())
            .andExpect(jsonPath("$.paths['/api/job-postings'].post.responses['400']")
                .exists())
            .andExpect(jsonPath("$.paths['/api/job-postings'].post.responses['200']")
                .doesNotExist())
            .andExpect(jsonPath("$.paths['/api/job-postings/{id}'].get.responses['404']")
                .exists())
            .andExpect(jsonPath("$.components.schemas.CreateJobPostingRequest")
                .exists())
            .andExpect(jsonPath(
                "$.components.schemas.CreateJobPostingRequest.properties.companyName.example"
            ).value("Example Technologies Kft."));

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
              "classification": "A",
              "reviewNote": null
            }
            """;
    }

    private JobPosting jobPosting(
        String roleTitle,
        String sourceUrl,
        Instant createdAt
    ) {
        return JobPosting.create(
            "Example Technologies Kft.",
            roleTitle,
            "Company careers",
            sourceUrl,
            null,
            "Budapest",
            WorkMode.HYBRID,
            LocalDate.of(2020, 1, 15),
            JobPostingClassification.A,
            null,
            createdAt
        );
    }
}
