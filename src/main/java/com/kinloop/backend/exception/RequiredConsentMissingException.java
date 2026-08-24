package com.kinloop.backend.exception;

public class RequiredConsentMissingException extends RuntimeException {
    public RequiredConsentMissingException() {
        super("All active required consents must be granted before generating activities");
    }
}
