package hu.seregergo.jobsearch.jobapplication.api;

import hu.seregergo.jobsearch.PostgreSqlIntegrationTest;
import hu.seregergo.jobsearch.jobapplication.domain.ApplicationStage;
import hu.seregergo.jobsearch.jobapplication.domain.JobApplication;
import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;
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
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MultipartUploadLimitIntegrationTests extends PostgreSqlIntegrationTest {

    private static final Instant BASE_TIME = Instant.parse("2026-07-30T08:00:00Z");

    @LocalServerPort
    private int port;

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
    void acceptsExactlyFiveMebibytesAndRejectsTheNextByteAtTheHttpBoundary()
        throws Exception {
        JobApplication acceptedApplication = saveApplication("Exact limit");
        byte[] exact = pdfOfSize((int) PdfDocument.MAX_SIZE_BYTES);

        HttpResponse<String> accepted = sendSubmission(
            acceptedApplication.getId(),
            exact
        );

        assertEquals(200, accepted.statusCode());
        assertEquals(1, cvRepository.count());

        JobApplication rejectedApplication = saveApplication("Over limit");
        byte[] oversized = pdfOfSize((int) PdfDocument.MAX_SIZE_BYTES + 1);

        HttpResponse<String> rejected = sendSubmission(
            rejectedApplication.getId(),
            oversized
        );

        assertEquals(400, rejected.statusCode());
        assertTrue(rejected.body().contains("CV_VALIDATION_FAILED"));
        assertEquals(1, cvRepository.count());
        assertEquals(
            ApplicationStage.PREPARING,
            applicationRepository.findById(rejectedApplication.getId())
                .orElseThrow()
                .getStage()
        );
    }

    private HttpResponse<String> sendSubmission(UUID applicationId, byte[] pdf)
        throws Exception {
        String boundary = "job-search-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, pdf);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(
                "http://127.0.0.1:" + port
                    + "/api/applications/" + applicationId + "/submit"
            ))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .header("Idempotency-Key", UUID.randomUUID().toString())
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
        return HttpClient.newHttpClient().send(
            request,
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private byte[] multipartBody(String boundary, byte[] pdf) {
        LocalDate today = LocalDate.now();
        ByteArrayOutputStream output = new ByteArrayOutputStream(
            pdf.length + 1_024
        );
        writeField(output, boundary, "submittedOn", today.toString());
        writeField(output, boundary, "nextAction", "Check for a response");
        writeField(output, boundary, "dueOn", today.plusDays(7).toString());
        writeField(output, boundary, "cvLanguage", "EN");
        output.writeBytes((
            "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"cv\"; "
                + "filename=\"CV.pdf\"\r\n"
                + "Content-Type: application/pdf\r\n\r\n"
        ).getBytes(StandardCharsets.US_ASCII));
        output.writeBytes(pdf);
        output.writeBytes(("\r\n--" + boundary + "--\r\n")
            .getBytes(StandardCharsets.US_ASCII));
        return output.toByteArray();
    }

    private void writeField(
        ByteArrayOutputStream output,
        String boundary,
        String name,
        String value
    ) {
        output.writeBytes((
            "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
                + value + "\r\n"
        ).getBytes(StandardCharsets.UTF_8));
    }

    private byte[] pdfOfSize(int size) {
        byte[] bytes = new byte[size];
        byte[] header = "%PDF-".getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(header, 0, bytes, 0, header.length);
        return bytes;
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
            "Finish the application",
            LocalDate.now().plusDays(3),
            null,
            BASE_TIME
        ));
    }
}
