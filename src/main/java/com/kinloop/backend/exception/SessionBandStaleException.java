package com.kinloop.backend.exception;
public class SessionBandStaleException extends RuntimeException {
    public SessionBandStaleException() { super("Questionnaire session age band is stale; re-fetch current questionnaire"); }
}
