package hu.seregergo.jobsearch.jobapplication.application;

public class InvalidSubmittedCvException extends RuntimeException {

    public InvalidSubmittedCvException(String message) {
        super(message);
    }

    public InvalidSubmittedCvException(String message, Throwable cause) {
        super(message, cause);
    }
}
