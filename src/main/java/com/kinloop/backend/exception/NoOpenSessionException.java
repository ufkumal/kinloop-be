package com.kinloop.backend.exception;
public class NoOpenSessionException extends RuntimeException {
    public NoOpenSessionException(Long id) { super("No open questionnaire session for child: " + id); }
}
