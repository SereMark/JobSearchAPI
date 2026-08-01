package hu.seregergo.jobsearch.jobapplication.domain;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfDocumentTests {

    @Test
    void normalizesTheNameComputesTheHashAndProtectsItsBytes() throws Exception {
        byte[] source = pdfBytes("original");
        PdfDocument document = PdfDocument.create(
            "  Re\u0301sume\u0301, Gergo (EN).PDF  ",
            source
        );

        source[0] = 'X';
        byte[] returned = document.bytes();
        returned[1] = 'X';

        assertEquals("Résumé, Gergo (EN).PDF", document.originalFileName());
        assertEquals(pdfBytes("original").length, document.sizeBytes());
        assertEquals(sha256(pdfBytes("original")), document.sha256());
        assertArrayEquals(pdfBytes("original"), document.bytes());
    }

    @Test
    void acceptsExactlyFiveMebibytes() {
        byte[] bytes = new byte[(int) PdfDocument.MAX_SIZE_BYTES];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, 5);

        PdfDocument document = PdfDocument.create("CV.pdf", bytes);

        assertEquals(PdfDocument.MAX_SIZE_BYTES, document.sizeBytes());
    }

    @Test
    void rejectsEmptyOversizedAndNonPdfContent() {
        byte[] oversized = new byte[(int) PdfDocument.MAX_SIZE_BYTES + 1];
        System.arraycopy("%PDF-".getBytes(StandardCharsets.US_ASCII), 0, oversized, 0, 5);

        assertThrows(
            IllegalArgumentException.class,
            () -> PdfDocument.create("CV.pdf", new byte[0])
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> PdfDocument.create("CV.pdf", oversized)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> PdfDocument.create(
                "CV.pdf",
                "not a pdf".getBytes(StandardCharsets.UTF_8)
            )
        );
    }

    @Test
    void rejectsUnsafeOrMisleadingFileNames() {
        byte[] pdf = pdfBytes("safe");

        for (String name : new String[] {
            "CV.txt",
            "../CV.pdf",
            "folder\\CV.pdf",
            "CV\nInjected.pdf",
            "CON.pdf",
            ".pdf"
        }) {
            assertThrows(
                IllegalArgumentException.class,
                () -> PdfDocument.create(name, pdf),
                name
            );
        }
        assertThrows(
            IllegalArgumentException.class,
            () -> PdfDocument.create("a".repeat(252) + ".pdf", pdf)
        );
    }

    private byte[] pdfBytes(String marker) {
        return ("%PDF-1.7\n" + marker + "\n%%EOF").getBytes(StandardCharsets.UTF_8);
    }

    private String sha256(byte[] value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(value)
        );
    }
}
