package com.kinloop.backend.exception;

public class UnsupportedChildAgeException extends RuntimeException {

    public UnsupportedChildAgeException(int ageMonths) {
        super("Unsupported child age in months: " + ageMonths);
    }
}
