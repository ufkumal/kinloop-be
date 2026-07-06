package com.kinloop.backend.exception;

import java.util.Map;

public record ApiErrorResponse(
        String error,
        int status,
        Map<String, String> fieldErrors
) {

    public ApiErrorResponse(String error, int status) {
        this(error, status, null);
    }
}
