package com.kinloop.backend.dto.questionnaire;
import com.kinloop.backend.entity.enums.QuestionType;
import java.util.List;
public record QuestionnaireQuestionResponse(String code, String body, String helperText, QuestionType type,
        boolean required, int displayOrder, List<QuestionOptionResponse> options, String answeredOptionCode) { }
