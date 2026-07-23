package com.kinloop.backend.service;

import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.repository.QuestionnaireSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class QuestionnaireSessionWriter {
    private final QuestionnaireSessionRepository sessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuestionnaireSession insert(QuestionnaireSession session) {
        return sessionRepository.saveAndFlush(session);
    }
}
