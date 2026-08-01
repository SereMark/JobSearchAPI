package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmittedCvFileValidatorTests {

    private final SubmittedCvFileValidator validator = new SubmittedCvFileValidator();

    @Test
    void acceptsAValidPdfAtTheExactSizeBoundary() {
        byte[] bytes = new byte[(int) PdfDocument.MAX_SIZE_BYTES];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 5);
        MockMultipartFile file = file("CV.pdf", "application/pdf", bytes);

        PdfDocument document = validator.validate(file);

        assertEquals(PdfDocument.MAX_SIZE_BYTES, document.sizeBytes());
    }

    @Test
    void rejectsMissingEmptyAndOversizedFiles() {
        byte[] oversized = new byte[(int) PdfDocument.MAX_SIZE_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, oversized, 0, 5);

        assertThrows(InvalidSubmittedCvException.class, () -> validator.validate(null));
        assertThrows(
            InvalidSubmittedCvException.class,
            () -> validator.validate(file("CV.pdf", "application/pdf", new byte[0]))
        );
        assertThrows(
            InvalidSubmittedCvException.class,
            () -> validator.validate(file("CV.pdf", "application/pdf", oversized))
        );
    }

    @Test
    void rejectsWrongContentTypeExtensionHeaderAndFileName() {
        byte[] validPdf = pdfBytes();

        assertThrows(
            InvalidSubmittedCvException.class,
            () -> validator.validate(file("CV.pdf", "APPLICATION/PDF", validPdf))
        );
        assertThrows(
            InvalidSubmittedCvException.class,
            () -> validator.validate(file("CV.txt", "application/pdf", validPdf))
        );
        assertThrows(
            InvalidSubmittedCvException.class,
            () -> validator.validate(file(
                "CV.pdf",
                "application/pdf",
                "plain text".getBytes(StandardCharsets.UTF_8)
            ))
        );
        assertThrows(
            InvalidSubmittedCvException.class,
            () -> validator.validate(file("../CV.pdf", "application/pdf", validPdf))
        );
    }

    private MockMultipartFile file(
        String fileName,
        String contentType,
        byte[] bytes
    ) {
        return new MockMultipartFile("cv", fileName, contentType, bytes);
    }

    private byte[] pdfBytes() {
        return "%PDF-1.7\n%%EOF".getBytes(StandardCharsets.US_ASCII);
    }
}
