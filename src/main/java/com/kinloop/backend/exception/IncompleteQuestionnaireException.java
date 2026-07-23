package com.kinloop.backend.exception;
import lombok.Getter;

import java.util.List;

@Getter
public class IncompleteQuestionnaireException extends RuntimeException {
    private final List<String> missingQuestionCodes;
    public IncompleteQuestionnaireException(List<String> codes) {
        super("Missing required question answers: " + String.join(", ", codes)); this.missingQuestionCodes = List.copyOf(codes);
    }
}
