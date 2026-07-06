package com.kinloop.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException {
//TODO it is a vulnerability we expose the sensitive data information with this message. give each error message securely.
    public EmailAlreadyExistsException() {
        super("Email already exists");
    }
}
