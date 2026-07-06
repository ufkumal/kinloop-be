package com.kinloop.backend.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException() {
        super("Account is not active");
    }
}
