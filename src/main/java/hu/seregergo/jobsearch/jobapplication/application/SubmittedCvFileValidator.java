package hu.seregergo.jobsearch.jobapplication.application;

import hu.seregergo.jobsearch.jobapplication.domain.PdfDocument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class SubmittedCvFileValidator {

    public PdfDocument validate(MultipartFile file) {
        if (file == null) {
            throw new InvalidSubmittedCvException("A PDF CV is required");
        }
        if (!MediaType.APPLICATION_PDF_VALUE.equals(file.getContentType())) {
            throw new InvalidSubmittedCvException(
                "The CV content type must be application/pdf"
            );
        }
        if (file.getSize() > PdfDocument.MAX_SIZE_BYTES) {
            throw new InvalidSubmittedCvException("The PDF CV must not exceed 5 MiB");
        }
        if (file.getOriginalFilename() == null) {
            throw new InvalidSubmittedCvException("The PDF CV needs a file name");
        }

        try {
            return PdfDocument.create(file.getOriginalFilename(), file.getBytes());
        } catch (IllegalArgumentException exception) {
            throw new InvalidSubmittedCvException(exception.getMessage(), exception);
        } catch (IOException exception) {
            throw new InvalidSubmittedCvException(
                "The PDF CV could not be read",
                exception
            );
        }
    }
}
