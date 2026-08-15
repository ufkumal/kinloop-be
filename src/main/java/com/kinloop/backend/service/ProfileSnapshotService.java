package com.kinloop.backend.service;

import com.kinloop.backend.entity.*;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.repository.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ProfileSnapshotService {
    private final QuestionnaireSessionRepository sessionRepository;
    private final ChildAnswerRepository answerRepository;
    private final ChildProfileSnapshotRepository snapshotRepository;

    /**
     * Folds history oldest-to-newest with latest-non-null-wins so attributes not asked by a later band carry forward.
     */
    @Transactional
    public ChildProfileSnapshot rebuild(QuestionnaireSession session) {
        ChildProfileSnapshot next = new ChildProfileSnapshot();
        List<GardnerPrior> priors = new ArrayList<>();
        for (QuestionnaireSession completed : sessionRepository
                .findByChildIdAndStatusOrderByCompletedAtAsc(session.getChildId(), SessionStatus.COMPLETED)) {
            for (ChildAnswer answer : answerRepository.findBySessionId(completed.getId())) {
                QuestionOption option = answer.getOption();
                if (option == null) continue;
                if (option.getDunnQuadrant() != null) next.setDunnQuadrant(option.getDunnQuadrant());
                if (option.getSeparationAnxiety() != null) next.setSeparationAnxiety(option.getSeparationAnxiety());
                if (option.getSocialOrientation() != null) next.setSocialOrientation(option.getSocialOrientation());
                if (option.getFocusBand() != null) next.setFocusBand(option.getFocusBand());
                if (option.getGardnerPriors() != null) priors.addAll(option.getGardnerPriors());
            }
        }
        snapshotRepository.findByChildIdAndCurrentTrue(session.getChildId()).ifPresent(current -> {
            current.setCurrent(false); snapshotRepository.saveAndFlush(current);
        });
        next.setChildId(session.getChildId()); next.setSession(session); next.setAgeBand(session.getAgeBand());
        next.setAgeMonthsAtCapture(session.getAgeMonthsAtStart()); next.setGardnerPriors(priors); next.setCurrent(true);
        return snapshotRepository.save(next);
    }
}
