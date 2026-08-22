package com.kinloop.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kinloop.backend.dto.feedback.FeedbackQuestionsResponse;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionOption;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.entity.enums.QuestionType;
import com.kinloop.backend.repository.QuestionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedbackQuestionServiceTest {

    @Mock private QuestionRepository questionRepository;

    private FeedbackQuestionService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackQuestionService(questionRepository);
    }

    @Test
    void returnsActiveFeedbackQuestionsWithTheirRenderingMetadata() {
        Question enjoyment = question(
                "FB_ENJOYMENT", "Çocuğun etkinlikten keyif aldı mı?",
                QuestionType.SINGLE_CHOICE, true, 1, null);
        enjoyment.setOptions(List.of(
                option("LIKED", "Yaptık, sevdi", 1),
                option("STRUGGLED", "Denedik, zorlandı", 2),
                option("DISLIKED", "Olmadı, sevmedi", 3)
        ));
        Question comment = question(
                "FB_COMMENT", "Deneyiminizi kendi cümlelerinizle paylaşın.",
                QuestionType.FREE_TEXT, false, 3, 2000);

        when(questionRepository.findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.FEEDBACK))
                .thenReturn(List.of(enjoyment, comment));

        FeedbackQuestionsResponse response = service.questions();

        assertEquals(2, response.questions().size());
        assertEquals("FB_ENJOYMENT", response.questions().get(0).code());
        assertTrue(response.questions().get(0).required());
        assertEquals(3, response.questions().get(0).options().size());
        assertEquals("LIKED", response.questions().get(0).options().get(0).code());
        assertEquals("FB_COMMENT", response.questions().get(1).code());
        assertFalse(response.questions().get(1).required());
        assertEquals(2000, response.questions().get(1).maxLength());
        verify(questionRepository).findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope.FEEDBACK);
    }

    private Question question(String code, String body, QuestionType type,
                              boolean required, int displayOrder, Integer maxLength) {
        Question question = new Question();
        question.setCode(code);
        question.setBody(body);
        question.setQuestionType(type);
        question.setScope(QuestionScope.FEEDBACK);
        question.setRequired(required);
        question.setDisplayOrder(displayOrder);
        question.setMaxLength(maxLength);
        question.setActive(true);
        return question;
    }

    private QuestionOption option(String code, String label, int displayOrder) {
        QuestionOption option = new QuestionOption();
        option.setCode(code);
        option.setLabel(label);
        option.setDisplayOrder(displayOrder);
        return option;
    }
}
