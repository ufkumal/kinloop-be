package com.kinloop.backend.service;

import com.kinloop.backend.dto.child.SessionSummaryResponse;
import com.kinloop.backend.exception.SessionAlreadyOpenException;
import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.enums.QuestionScope;
import com.kinloop.backend.entity.enums.TriggerReason;
import com.kinloop.backend.repository.QuestionRepository;
import com.kinloop.backend.repository.QuestionnaireSessionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionnaireSessionService {

    private final QuestionnaireSessionRepository questionnaireSessionRepository;
    private final QuestionRepository questionRepository;

    /**
     * Creates the initial session and maps the DB uniqueness race to a business exception so clients receive 409.
     */
    @Transactional
    public QuestionnaireSession openInitialSession(Long childId, int ageMonths) {
        try {
            QuestionnaireSession session = QuestionnaireSession.open(childId, ageMonths, TriggerReason.INITIAL);
            return questionnaireSessionRepository.save(session);
        } catch (DataIntegrityViolationException ex) {
            throw new SessionAlreadyOpenException(childId, ex);
        }
    }

    @Transactional(readOnly = true)
    public SessionSummaryResponse summarise(QuestionnaireSession session, int ageMonths, int answeredCount) {
        List<Question> questions = questionRepository.findForAge(ageMonths, QuestionScope.CHILD);
        String nextQuestionCode = answeredCount == 0 && !questions.isEmpty() ? questions.getFirst().getCode() : null;
        return new SessionSummaryResponse(
                session.getId(),
                session.getStatus(),
                session.getTriggerReason(),
                questions.size(),
                answeredCount,
                nextQuestionCode
        );
    }
}
