package com.kinloop.backend.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.error(ex.getMessage());
        return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailNotVerified(EmailNotVerifiedException ex) {
        log.error(ex.getMessage());
        return error(HttpStatus.FORBIDDEN, "please verify your email");
    }

    @ExceptionHandler(AccountNotActiveException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountNotActive(AccountNotActiveException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({InvalidTokenException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse("Validation failed", status.value(), fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnsupportedChildAgeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedChildAge(UnsupportedChildAgeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(SessionAlreadyOpenException.class)
    public ResponseEntity<ApiErrorResponse> handleSessionAlreadyOpen(SessionAlreadyOpenException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ChildNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleChildNotFound(ChildNotFoundException ex) { return error(HttpStatus.NOT_FOUND, ex.getMessage()); }

    @ExceptionHandler({NoOpenSessionException.class, SessionBandStaleException.class})
    public ResponseEntity<ApiErrorResponse> handleQuestionnaireConflict(RuntimeException ex) { return error(HttpStatus.CONFLICT, ex.getMessage()); }

    @ExceptionHandler({QuestionNotInSessionException.class, InvalidOptionException.class})
    public ResponseEntity<ApiErrorResponse> handleQuestionnaireBadRequest(RuntimeException ex) { return error(HttpStatus.BAD_REQUEST, ex.getMessage()); }

    @ExceptionHandler(IncompleteQuestionnaireException.class)
    public ResponseEntity<ApiErrorResponse> handleIncomplete(IncompleteQuestionnaireException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        for (String code : ex.getMissingQuestionCodes()) details.put(code, "Answer required");
        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(new ApiErrorResponse(ex.getMessage(), status.value(), details));
    }

    @ExceptionHandler(DailyPlanNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDailyPlanNotFound(DailyPlanNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DailyPlanItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleDailyPlanItemNotFound(DailyPlanItemNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(FeedbackAlreadySubmittedException.class)
    public ResponseEntity<ApiErrorResponse> handleFeedbackAlreadySubmitted(FeedbackAlreadySubmittedException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ActivityNotInDailyPlanException.class)
    public ResponseEntity<ApiErrorResponse> handleActivityNotInDailyPlan(ActivityNotInDailyPlanException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse>  handleIllegalState(IllegalStateException ex) {
        return  error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message, status.value()));
    }
}
