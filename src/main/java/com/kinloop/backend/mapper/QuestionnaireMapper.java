package com.kinloop.backend.mapper;

import com.kinloop.backend.dto.questionnaire.*;
import com.kinloop.backend.entity.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class QuestionnaireMapper {
    public CurrentQuestionnaireResponse toCurrent(QuestionnaireSession session, List<Question> questions,
                                                   List<ChildAnswer> answers) {
        Map<Long, ChildAnswer> byQuestion = answers.stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), Function.identity()));
        List<QuestionnaireQuestionResponse> responses = questions.stream().map(q -> {
            ChildAnswer answer = byQuestion.get(q.getId());
            return new QuestionnaireQuestionResponse(q.getCode(), q.getBody(), q.getQuestionType(),
                    q.isRequired(), q.getDisplayOrder(), q.getOptions().stream()
                    .map(o -> new QuestionOptionResponse(o.getCode(), o.getLabel(), o.getDisplayOrder())).toList(),
                    answer == null || answer.getOption() == null ? null : answer.getOption().getCode());
        }).toList();
        String next = questions.stream().filter(Question::isRequired).filter(q -> !byQuestion.containsKey(q.getId()))
                .map(Question::getCode).findFirst().orElse(null);
        return new CurrentQuestionnaireResponse(session.getId(), session.getStatus(), session.getTriggerReason(),
                session.getAgeBand(), session.getAgeMonthsAtStart(), questions.size(), answers.size(), next, responses);
    }

    public QuestionnaireCompleteResponse toComplete(QuestionnaireSession session, ChildProfileSnapshot snapshot) {
        return new QuestionnaireCompleteResponse(session.getId(), session.getStatus(), snapshot.getId(),
                session.getAgeBand(), session.getAgeMonthsAtStart());
    }
}
