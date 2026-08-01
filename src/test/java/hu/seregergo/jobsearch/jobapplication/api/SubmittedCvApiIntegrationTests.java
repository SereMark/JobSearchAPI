package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SubmittedCvApiIntegrationTests extends PostgreSqlIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-30T08:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        idempotencyRepository.deleteAll();
        cvRepository.deleteAll();
        applicationRepository.deleteAll();
        jobPostingRepository.deleteAll();
    }

    @Test
    void storesOnlyCvMetadataInJsonAndDownloadsTheExactBytesAfterClosure()
        throws Exception {
        JobApplication application = saveApplication("CV included", "PRIVATE NOTE");
        LocalDate today = LocalDate.now();
        byte[] pdf = pdfBytes("exact-original");
        String expectedHash = sha256(pdf);

        MvcResult submit = mockMvc.perform(submitRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "Check for a response",
                "EN",
                pdfFile("Gergő CV, 2026 (EN).pdf", pdf)
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.applicationId").value(application.getId().toString()))
            .andExpect(jsonPath("$.stage").value("SUBMITTED"))
            .andExpect(jsonPath("$.submittedCv.sentOn").value(today.toString()))
            .andExpect(jsonPath("$.submittedCv.language").value("EN"))
            .andExpect(jsonPath("$.submittedCv.originalFileName")
                .value("Gergő CV, 2026 (EN).pdf"))
            .andExpect(jsonPath("$.submittedCv.sizeBytes").value(pdf.length))
            .andExpect(jsonPath("$.submittedCv.sha256").value(expectedHash))
            .andExpect(jsonPath("$.submittedCv.recordedAt").isNotEmpty())
            .andExpect(jsonPath("$.submittedCv.bytes").doesNotExist())
            .andExpect(jsonPath("$.note").doesNotExist())
            .andReturn();

        String submitBody = submit.getResponse().getContentAsString();
        assertFalse(submitBody.contains("PRIVATE NOTE"));
        assertFalse(submitBody.contains("exact-original"));
        assertFalse(idempotencyRepository.findAll().getFirst().getResponseBody()
            .contains("bytes"));
        assertArrayEquals(
            pdf,
            jdbcTemplate.queryForObject(
                "SELECT bytes FROM submitted_cvs WHERE application_id = ?",
                byte[].class,
                application.getId()
            )
        );

        mockMvc.perform(get("/api/applications/{id}", application.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submittedCv.sha256").value(expectedHash))
            .andExpect(jsonPath("$.submittedCv.bytes").doesNotExist());

        mockMvc.perform(get("/api/applications"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].submittedCv.sha256").value(expectedHash))
            .andExpect(jsonPath("$[0].submittedCv.bytes").doesNotExist());

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get(
                "/api/applications/{id}/submitted-cv",
                application.getId()
            ))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(content().bytes(pdf))
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                containsString("attachment")
            ))
            .andExpect(header().string(
                HttpHeaders.CONTENT_DISPOSITION,
                containsString("Gerg")
            ));
    }

    @Test
    void recordsOneCvLaterWithoutChangingSubmissionAndReplaysAfterClosure()
        throws Exception {
        JobApplication application = saveApplication("CV recorded later", null);
        LocalDate today = LocalDate.now();
        UUID submitKey = UUID.randomUUID();
        MvcResult submit = mockMvc.perform(submitRequest(
                application.getId(),
                submitKey,
                today,
                "Follow up",
                null,
                null
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submittedCv").doesNotExist())
            .andReturn();
        String submissionUpdatedAt = json(submit).get("updatedAt").stringValue();

        mockMvc.perform(get(
                "/api/applications/{id}/submitted-cv",
                application.getId()
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("SUBMITTED_CV_NOT_FOUND"));

        UUID recordKey = UUID.randomUUID();
        byte[] pdf = pdfBytes("recorded-later");
        MvcResult recorded = mockMvc.perform(recordCvRequest(
                application.getId(),
                recordKey,
                today,
                "HU",
                pdfFile("Önéletrajz 2026.pdf", pdf)
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sentOn").value(today.toString()))
            .andExpect(jsonPath("$.language").value("HU"))
            .andExpect(jsonPath("$.sha256").value(sha256(pdf)))
            .andReturn();
        String originalResponse = recorded.getResponse().getContentAsString();

        mockMvc.perform(get("/api/applications/{id}", application.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.submittedOn").value(today.toString()))
            .andExpect(jsonPath("$.updatedAt").value(submissionUpdatedAt))
            .andExpect(jsonPath("$.submittedCv.sentOn").value(today.toString()));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson()))
            .andExpect(status().isOk());

        MvcResult replay = mockMvc.perform(recordCvRequest(
                application.getId(),
                recordKey,
                today,
                "HU",
                pdfFile("Önéletrajz 2026.pdf", pdf)
            ))
            .andExpect(status().isOk())
            .andReturn();
        assertEquals(originalResponse, replay.getResponse().getContentAsString());

        mockMvc.perform(recordCvRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "HU",
                pdfFile("Önéletrajz 2026.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
        assertEquals(1, cvRepository.count());
        assertEquals(2, idempotencyRepository.count());
    }

    @Test
    void replaysTheOriginalSubmitReceiptAndRejectsAllOtherKeyReuse()
        throws Exception {
        JobApplication application = saveApplication("Idempotent submit", "PRIVATE NOTE");
        JobApplication otherApplication = saveApplication("Other application", null);
        LocalDate today = LocalDate.now();
        UUID key = UUID.randomUUID();
        byte[] pdf = pdfBytes("idempotent");

        MvcResult first = mockMvc.perform(submitRequest(
                application.getId(),
                key,
                today,
                "  Re\u0301sume\u0301 follow-up  ",
                "EN",
                pdfFile("  Re\u0301sume\u0301.pdf  ", pdf)
            ))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nextAction").value("Résumé follow-up"))
            .andReturn();
        String originalResponse = first.getResponse().getContentAsString();

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson()))
            .andExpect(status().isOk());

        MvcResult replay = mockMvc.perform(submitRequest(
                application.getId(),
                key,
                today,
                "Résumé follow-up",
                "EN",
                pdfFile("Résumé.pdf", pdf)
            ))
            .andExpect(status().isOk())
            .andReturn();
        assertEquals(originalResponse, replay.getResponse().getContentAsString());

        mockMvc.perform(submitRequest(
                application.getId(),
                key,
                today,
                "Different follow-up",
                "EN",
                pdfFile("Résumé.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(submitRequest(
                otherApplication.getId(),
                key,
                today,
                "Résumé follow-up",
                "EN",
                pdfFile("Résumé.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(submitRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "Résumé follow-up",
                "EN",
                pdfFile("Résumé.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        mockMvc.perform(recordCvRequest(
                application.getId(),
                key,
                today,
                "EN",
                pdfFile("Résumé.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));

        assertEquals(1, idempotencyRepository.count());
        var storedRecord = idempotencyRepository.findAll().getFirst();
        String storedResponse = storedRecord.getResponseBody();
        assertEquals(200, storedRecord.getResponseStatus());
        assertEquals(originalResponse, storedResponse);
        assertFalse(storedResponse.contains("PRIVATE NOTE"));
        assertFalse(storedResponse.contains("bytes"));
        assertFalse(storedResponse.contains("idempotent"));
    }

    @Test
    void rejectsInvalidMultipartPairsFilesDatesHeadersAndStates() throws Exception {
        JobApplication application = saveApplication("Validation", null);
        LocalDate today = LocalDate.now();
        byte[] pdf = pdfBytes("valid");

        mockMvc.perform(submitRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "Follow up",
                null,
                pdfFile("CV.pdf", pdf)
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(submitRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "Follow up",
                "EN",
                null
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(submitRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "Follow up",
                "EN",
                new MockMultipartFile("cv", "CV.pdf", "text/plain", pdf)
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CV_VALIDATION_FAILED"));

        mockMvc.perform(multipart("/api/applications/{id}/submit", application.getId())
                .param("submittedOn", today.toString())
                .param("nextAction", "Follow up")
                .param("dueOn", today.plusDays(7).toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(multipart("/api/applications/{id}/submit", application.getId())
                .header("Idempotency-Key", "not-a-uuid")
                .param("submittedOn", today.toString())
                .param("nextAction", "Follow up")
                .param("dueOn", today.plusDays(7).toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));

        mockMvc.perform(get(
                "/api/applications/{id}/submitted-cv",
                UUID.randomUUID()
            ))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("APPLICATION_NOT_FOUND"));

        mockMvc.perform(submitRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "Follow up",
                null,
                null
            ))
            .andExpect(status().isOk());

        mockMvc.perform(recordCvRequest(
                application.getId(),
                UUID.randomUUID(),
                today.minusDays(1),
                "EN",
                pdfFile("CV.pdf", pdf)
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        mockMvc.perform(recordCvRequest(
                application.getId(),
                UUID.randomUUID(),
                today.plusDays(1),
                "EN",
                pdfFile("CV.pdf", pdf)
            ))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        JobApplication preparing = saveApplication("Still preparing", null);
        mockMvc.perform(recordCvRequest(
                preparing.getId(),
                UUID.randomUUID(),
                today,
                "EN",
                pdfFile("CV.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_STATE_CONFLICT"));

        mockMvc.perform(put("/api/applications/{id}/workflow", application.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(closedWorkflowJson()))
            .andExpect(status().isOk());
        mockMvc.perform(recordCvRequest(
                application.getId(),
                UUID.randomUUID(),
                today,
                "EN",
                pdfFile("CV.pdf", pdf)
            ))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("APPLICATION_STATE_CONFLICT"));

        assertEquals(0, cvRepository.count());
        assertEquals(1, idempotencyRepository.count());
    }

    @Test
    void serializesConcurrentSubmissionsIntoOneEffectOrOneConflict()
        throws Exception {
        LocalDate today = LocalDate.now();
        JobApplication replayApplication = saveApplication("Concurrent replay", null);
        UUID sharedKey = UUID.randomUUID();

        List<MvcResult> replayResults = runConcurrently(
            () -> mockMvc.perform(submitRequest(
                replayApplication.getId(),
                sharedKey,
                today,
                "Follow up",
                null,
                null
            )).andReturn(),
            () -> mockMvc.perform(submitRequest(
                replayApplication.getId(),
                sharedKey,
                today,
                "Follow up",
                null,
                null
            )).andReturn()
        );

        assertEquals(List.of(200, 200), statuses(replayResults));
        assertEquals(
            replayResults.get(0).getResponse().getContentAsString(),
            replayResults.get(1).getResponse().getContentAsString()
        );
        assertEquals(1, idempotencyRepository.count());

        JobApplication conflictApplication = saveApplication(
            "Concurrent conflict",
            null
        );
        List<MvcResult> conflictResults = runConcurrently(
            () -> mockMvc.perform(submitRequest(
                conflictApplication.getId(),
                UUID.randomUUID(),
                today,
                "First follow-up",
                null,
                null
            )).andReturn(),
            () -> mockMvc.perform(submitRequest(
                conflictApplication.getId(),
                UUID.randomUUID(),
                today,
                "Second follow-up",
                null,
                null
            )).andReturn()
        );

        assertEquals(List.of(200, 409), statuses(conflictResults));
        MvcResult conflict = conflictResults.stream()
            .filter(result -> result.getResponse().getStatus() == 409)
            .findFirst()
            .orElseThrow();
        assertTrue(conflict.getResponse().getContentAsString()
            .contains("IDEMPOTENCY_CONFLICT"));
        assertEquals(2, idempotencyRepository.count());

        JobApplication firstApplication = saveApplication("Global key one", null);
        JobApplication secondApplication = saveApplication("Global key two", null);
        UUID globalKey = UUID.randomUUID();
        List<MvcResult> globalKeyResults = runConcurrently(
            () -> mockMvc.perform(submitRequest(
                firstApplication.getId(),
                globalKey,
                today,
                "First application follow-up",
                null,
                null
            )).andReturn(),
            () -> mockMvc.perform(submitRequest(
                secondApplication.getId(),
                globalKey,
                today,
                "Second application follow-up",
                null,
                null
            )).andReturn()
        );

        assertEquals(List.of(200, 409), statuses(globalKeyResults));
        long submittedApplications = List.of(firstApplication, secondApplication)
            .stream()
            .map(application -> applicationRepository.findById(application.getId())
                .orElseThrow())
            .filter(application -> application.getStage() == ApplicationStage.SUBMITTED)
            .count();
        assertEquals(1, submittedApplications);
        assertEquals(3, idempotencyRepository.count());
    }

    private List<MvcResult> runConcurrently(
        RequestCall first,
        RequestCall second
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<MvcResult>> futures = new ArrayList<>();
            for (RequestCall call : List.of(first, second)) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return call.perform();
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            List<MvcResult> results = new ArrayList<>();
            for (Future<MvcResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private List<Integer> statuses(List<MvcResult> results) {
        return results.stream()
            .map(result -> result.getResponse().getStatus())
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray());
    }

    private MockMultipartHttpServletRequestBuilder submitRequest(
        UUID applicationId,
        UUID idempotencyKey,
        LocalDate submittedOn,
        String nextAction,
        String cvLanguage,
        MockMultipartFile cv
    ) {
        MockMultipartHttpServletRequestBuilder request = multipart(
            "/api/applications/{id}/submit",
            applicationId
        )
            .header("Idempotency-Key", idempotencyKey)
            .param("submittedOn", submittedOn.toString())
            .param("nextAction", nextAction)
            .param("dueOn", submittedOn.plusDays(7).toString());
        if (cvLanguage != null) {
            request.param("cvLanguage", cvLanguage);
        }
        if (cv != null) {
            request.file(cv);
        }
        return request;
    }

    private MockMultipartHttpServletRequestBuilder recordCvRequest(
        UUID applicationId,
        UUID idempotencyKey,
        LocalDate sentOn,
        String cvLanguage,
        MockMultipartFile cv
    ) {
        return multipart("/api/applications/{id}/record-sent-cv", applicationId)
            .file(cv)
            .header("Idempotency-Key", idempotencyKey)
            .param("sentOn", sentOn.toString())
            .param("cvLanguage", cvLanguage);
    }

    private MockMultipartFile pdfFile(String fileName, byte[] bytes) {
        return new MockMultipartFile(
            "cv",
            fileName,
            MediaType.APPLICATION_PDF_VALUE,
            bytes
        );
    }

    private byte[] pdfBytes(String marker) {
        return ("%PDF-1.7\n" + marker + "\n%%EOF")
            .getBytes(StandardCharsets.UTF_8);
    }

    private String sha256(byte[] value) throws Exception {
        return java.util.HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(value)
        );
    }

    private JobApplication saveApplication(String roleTitle, String note) {
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
            "Finish the application",
            LocalDate.now().plusDays(3),
            note,
            BASE_TIME
        ));
    }

    private String closedWorkflowJson() {
        return """
            {
              "stage": "FINAL",
              "stageLabel": null,
              "nextAction": null,
              "dueOn": null,
              "outcome": "REJECTED",
              "note": "Application closed"
            }
            """;
    }

    @FunctionalInterface
    private interface RequestCall {

        MvcResult perform() throws Exception;
    }
}
