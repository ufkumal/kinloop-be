package com.kinloop.backend.exception;
public class QuestionNotInSessionException extends RuntimeException {
    public QuestionNotInSessionException(String code) { super("Question is not in this session: " + code); }
}
