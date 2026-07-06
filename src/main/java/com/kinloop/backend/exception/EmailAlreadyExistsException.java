package com.kinloop.backend.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("You can not do this transaction");
    }
}
