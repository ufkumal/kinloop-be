package com.kinloop.backend.exception;
public class InvalidOptionException extends RuntimeException {
    public InvalidOptionException(String code) { super("Invalid option: " + code); }
}
