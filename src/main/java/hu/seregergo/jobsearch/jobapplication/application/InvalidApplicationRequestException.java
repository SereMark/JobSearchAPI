package hu.seregergo.jobsearch.jobapplication.application;

public class InvalidApplicationRequestException extends RuntimeException {

    public InvalidApplicationRequestException(String message) {
        super(message);
    }
}
