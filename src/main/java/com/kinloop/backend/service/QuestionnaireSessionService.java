package com.kinloop.backend.service;

import com.kinloop.backend.dto.questionnaire.*;
import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.*;
import com.kinloop.backend.exception.*;
import com.kinloop.backend.mapper.QuestionnaireMapper;
import com.kinloop.backend.repository.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class QuestionnaireSessionService {
    private final QuestionnaireSessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository optionRepository;
    private final ChildAnswerRepository answerRepository;
    private final ProfileSnapshotService snapshotService;
    private final QuestionnaireMapper mapper;
    private final QuestionnaireSessionWriter sessionWriter;

    @Transactional
    public QuestionnaireSession openInitialSession(Long childId, int ageMonths) {
        return sessionRepository.save(QuestionnaireSession.open(childId, ageMonths, TriggerReason.INITIAL));
    }

    @Transactional(readOnly = true)
    public com.kinloop.backend.dto.child.SessionSummaryResponse summarise(QuestionnaireSession session, int answeredCount) {
        List<Question> questions = questions(session);
        return new com.kinloop.backend.dto.child.SessionSummaryResponse(session.getId(), session.getStatus(),
                session.getTriggerReason(), questions.size(), answeredCount,
                questions.isEmpty() ? null : questions.getFirst().getCode());
    }

    @Transactional
    public CurrentQuestionnaireResponse current(Child child) {
        int ageMonths = child.ageInMonths(LocalDate.now());
        AgeBand band = AgeBand.resolve(ageMonths);
        Optional<QuestionnaireSession> existing = sessionRepository.findByChildIdAndStatus(child.getId(), SessionStatus.IN_PROGRESS);
        QuestionnaireSession session;
        if (existing.isPresent() && existing.get().getAgeBand() == band) session = existing.get();
        else {
            existing.ifPresent(this::abandonBandSpecificSession);
            TriggerReason reason = sessionRepository.existsByChildIdAndStatus(child.getId(), SessionStatus.COMPLETED)
                    ? TriggerReason.AGE_BAND : TriggerReason.INITIAL;
            try {
                QuestionnaireSession fresh = QuestionnaireSession.open(child.getId(), ageMonths, reason);
                session = existing.isPresent() ? sessionRepository.saveAndFlush(fresh) : sessionWriter.insert(fresh);
            } catch (DataIntegrityViolationException race) {
                session = sessionRepository.findByChildIdAndStatus(child.getId(), SessionStatus.IN_PROGRESS).orElseThrow(() -> race);
            }
        }
        return response(session);
    }

    /**
     * Unconfirmed answers are band-specific, so abandoning prevents outgrown questions from leaking into a new band.
     */
    private void abandonBandSpecificSession(QuestionnaireSession stale) {
        stale.markAbandoned();
        sessionRepository.saveAndFlush(stale);
    }

    /** Answers are upserted only in an open session so review edits stay idempotent while completed history stays final. */
    @Transactional
    public CurrentQuestionnaireResponse answer(Child child, String questionCode, String optionCode) {
        QuestionnaireSession session = open(child.getId());
        assertCurrentBand(child, session);
        Question question = questions(session).stream().filter(q -> q.getCode().equals(questionCode)).findFirst()
                .orElseThrow(() -> new QuestionNotInSessionException(questionCode));
        QuestionOption option = optionRepository.findByQuestionIdAndCode(question.getId(), optionCode)
                .orElseThrow(() -> new InvalidOptionException(optionCode));
        ChildAnswer answer = answerRepository.findBySessionIdAndQuestionId(session.getId(), question.getId())
                .map(existing -> { existing.replaceWithOption(option); return existing; })
                .orElseGet(() -> ChildAnswer.ofOption(session, question, option));
        answerRepository.save(answer);
        return response(session);
    }

    @Transactional
    public QuestionnaireCompleteResponse complete(Child child) {
        QuestionnaireSession session = open(child.getId());
        assertCurrentBand(child, session);
        List<Question> questions = questions(session);
        Set<Long> answered = new HashSet<>();
        answerRepository.findBySessionId(session.getId()).forEach(a -> answered.add(a.getQuestion().getId()));
        List<String> missing = questions.stream().filter(Question::isRequired).filter(q -> !answered.contains(q.getId()))
                .map(Question::getCode).toList();
        if (!missing.isEmpty()) throw new IncompleteQuestionnaireException(missing);
        session.markCompleted(); sessionRepository.saveAndFlush(session);
        ChildProfileSnapshot snapshot = snapshotService.rebuild(session);
        return mapper.toComplete(session, snapshot);
    }

    private QuestionnaireSession open(Long childId) {
        return sessionRepository.findByChildIdAndStatus(childId, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new NoOpenSessionException(childId));
    }
    private void assertCurrentBand(Child child, QuestionnaireSession session) {
        if (session.getAgeBand() != AgeBand.resolve(child.ageInMonths(LocalDate.now()))) throw new SessionBandStaleException();
    }
    private List<Question> questions(QuestionnaireSession session) {
        return questionRepository.findForAge(session.getAgeMonthsAtStart(), QuestionScope.CHILD);
    }
    private CurrentQuestionnaireResponse response(QuestionnaireSession session) {
        return mapper.toCurrent(session, questions(session), answerRepository.findBySessionId(session.getId()));
    }
}
