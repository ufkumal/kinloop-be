package com.kinloop.backend.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Email is not verified");
    }
}
