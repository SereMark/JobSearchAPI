package hu.seregergo.jobsearch.jobapplication.application;

import java.util.Objects;

public record SubmittedCvDownload(String originalFileName, byte[] bytes) {

    public SubmittedCvDownload {
        Objects.requireNonNull(originalFileName, "originalFileName must not be null");
        bytes = Objects.requireNonNull(bytes, "bytes must not be null").clone();
    }

    @Override
    public byte[] bytes() {
        return bytes.clone();
    }
}
