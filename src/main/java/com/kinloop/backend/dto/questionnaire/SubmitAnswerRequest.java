package com.kinloop.backend.dto.questionnaire;
import jakarta.validation.constraints.NotBlank;
public record SubmitAnswerRequest(@NotBlank String optionCode) { }
