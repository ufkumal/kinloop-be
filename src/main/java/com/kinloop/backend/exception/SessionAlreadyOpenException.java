package com.kinloop.backend.exception;

public class SessionAlreadyOpenException extends RuntimeException {

    public SessionAlreadyOpenException(Long childId, Throwable cause) {
        super("An in-progress questionnaire session already exists for childId=" + childId, cause);
    }
}
