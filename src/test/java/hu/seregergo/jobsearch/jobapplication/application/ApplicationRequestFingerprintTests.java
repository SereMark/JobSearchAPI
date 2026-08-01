package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.CvLanguage;
import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationRequestFingerprintTests {

    private static final UUID APPLICATION_ID = UUID.fromString(
        "00000000-0000-0000-0000-000000000001"
    );

    private final ApplicationRequestFingerprint fingerprint =
        new ApplicationRequestFingerprint(new ObjectMapper());

    @Test
    void fingerprintsSubmitWithFixedOrderNormalizedTextAndThePdfHashOnly()
        throws Exception {
        PdfDocument pdf = PdfDocument.create(
            "  Re\u0301sume\u0301.pdf  ",
            "%PDF-1.7\nprivate bytes".getBytes(StandardCharsets.UTF_8)
        );
        SubmitApplicationCommand command = new SubmitApplicationCommand(
            LocalDate.of(2026, 8, 1),
            "  Re\u0301sume\u0301 follow-up  ",
            LocalDate.of(2026, 8, 8),
            CvLanguage.EN,
            pdf
        );
        String canonicalJson = "{"
            + "\"operation\":\"SUBMIT\","
            + "\"applicationId\":\"00000000-0000-0000-0000-000000000001\","
            + "\"submittedOn\":\"2026-08-01\","
            + "\"nextAction\":\"Résumé follow-up\","
            + "\"dueOn\":\"2026-08-08\","
            + "\"submittedCvIncluded\":true,"
            + "\"cvLanguage\":\"EN\","
            + "\"originalFileName\":\"Résumé.pdf\","
            + "\"cvSha256\":\"" + pdf.sha256() + "\""
            + "}";

        assertEquals(sha256(canonicalJson), fingerprint.forSubmit(
            APPLICATION_ID,
            command
        ));
    }

    @Test
    void fingerprintsMissingOptionalCvFieldsAsExplicitNulls() throws Exception {
        SubmitApplicationCommand command = new SubmitApplicationCommand(
            LocalDate.of(2026, 8, 1),
            "Follow up",
            LocalDate.of(2026, 8, 8),
            null,
            null
        );
        String canonicalJson = "{"
            + "\"operation\":\"SUBMIT\","
            + "\"applicationId\":\"00000000-0000-0000-0000-000000000001\","
            + "\"submittedOn\":\"2026-08-01\","
            + "\"nextAction\":\"Follow up\","
            + "\"dueOn\":\"2026-08-08\","
            + "\"submittedCvIncluded\":false,"
            + "\"cvLanguage\":null,"
            + "\"originalFileName\":null,"
            + "\"cvSha256\":null"
            + "}";

        assertEquals(sha256(canonicalJson), fingerprint.forSubmit(
            APPLICATION_ID,
            command
        ));
    }

    @Test
    void fingerprintsLaterCvRecordingWithItsDistinctOperationShape()
        throws Exception {
        PdfDocument pdf = PdfDocument.create(
            "CV.pdf",
            "%PDF-1.7\ncontent".getBytes(StandardCharsets.UTF_8)
        );
        RecordSentCvCommand command = new RecordSentCvCommand(
            LocalDate.of(2026, 8, 1),
            CvLanguage.HU,
            pdf
        );
        String canonicalJson = "{"
            + "\"operation\":\"RECORD_SENT_CV\","
            + "\"applicationId\":\"00000000-0000-0000-0000-000000000001\","
            + "\"sentOn\":\"2026-08-01\","
            + "\"cvLanguage\":\"HU\","
            + "\"originalFileName\":\"CV.pdf\","
            + "\"cvSha256\":\"" + pdf.sha256() + "\""
            + "}";

        assertEquals(sha256(canonicalJson), fingerprint.forRecordSentCv(
            APPLICATION_ID,
            command
        ));
    }

    private String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))
        );
    }
}
