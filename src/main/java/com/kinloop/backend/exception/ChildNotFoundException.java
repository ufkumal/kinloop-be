package com.kinloop.backend.exception;
public class ChildNotFoundException extends RuntimeException {
    public ChildNotFoundException(Long id) { super("Child not found: " + id); }
}
