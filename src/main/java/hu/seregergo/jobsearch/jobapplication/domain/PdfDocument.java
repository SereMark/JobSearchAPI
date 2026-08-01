package hu.seregergo.jobsearch.jobapplication.domain;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class PdfDocument {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    private static final byte[] PDF_HEADER = {'%', 'P', 'D', 'F', '-'};

    private final String originalFileName;
    private final byte[] bytes;
    private final String sha256;

    private PdfDocument(String originalFileName, byte[] bytes) {
        this.originalFileName = normalizeFileName(originalFileName);
        this.bytes = validateAndCopy(bytes);
        this.sha256 = sha256(this.bytes);
    }

    public static PdfDocument create(String originalFileName, byte[] bytes) {
        return new PdfDocument(originalFileName, bytes);
    }

    public String originalFileName() {
        return originalFileName;
    }

    public long sizeBytes() {
        return bytes.length;
    }

    public String sha256() {
        return sha256;
    }

    public byte[] bytes() {
        return bytes.clone();
    }

    private static String normalizeFileName(String value) {
        String fileName = Normalizer.normalize(
            Objects.requireNonNull(value, "originalFileName must not be null").strip(),
            Normalizer.Form.NFC
        );
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("originalFileName must not be blank");
        }
        if (fileName.length() > 255) {
            throw new IllegalArgumentException(
                "originalFileName must not exceed 255 characters"
            );
        }
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new IllegalArgumentException("originalFileName must end with .pdf");
        }

        String baseName = fileName.substring(0, fileName.length() - 4);
        if (baseName.isBlank() || baseName.endsWith(".")) {
            throw new IllegalArgumentException("originalFileName is not safe");
        }
        for (int offset = 0; offset < fileName.length();) {
            int codePoint = fileName.codePointAt(offset);
            int characterType = Character.getType(codePoint);
            boolean unsafe = Character.isISOControl(codePoint)
                || characterType == Character.FORMAT
                || characterType == Character.LINE_SEPARATOR
                || characterType == Character.PARAGRAPH_SEPARATOR
                || "<>:\"/\\|?*".indexOf(codePoint) >= 0;
            if (unsafe) {
                throw new IllegalArgumentException("originalFileName is not safe");
            }
            offset += Character.charCount(codePoint);
        }
        String windowsBaseName = baseName.split("\\.", 2)[0]
            .toUpperCase(Locale.ROOT);
        if (Set.of("CON", "PRN", "AUX", "NUL").contains(windowsBaseName)
            || windowsBaseName.matches("COM[1-9]|LPT[1-9]")) {
            throw new IllegalArgumentException("originalFileName is not safe");
        }
        return fileName;
    }

    private static byte[] validateAndCopy(byte[] value) {
        byte[] copy = Objects.requireNonNull(value, "bytes must not be null").clone();
        if (copy.length == 0) {
            throw new IllegalArgumentException("PDF must not be empty");
        }
        if (copy.length > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("PDF must not exceed 5 MiB");
        }
        if (copy.length < PDF_HEADER.length
            || !Arrays.equals(PDF_HEADER, Arrays.copyOf(copy, PDF_HEADER.length))) {
            throw new IllegalArgumentException("File content must start with %PDF-");
        }
        return copy;
    }

    private static String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value)
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
