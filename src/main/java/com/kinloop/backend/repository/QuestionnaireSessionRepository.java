package com.kinloop.backend.repository;

import com.kinloop.backend.entity.QuestionnaireSession;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kinloop.backend.entity.enums.SessionStatus;
import java.util.List;
import java.util.Optional;

public interface QuestionnaireSessionRepository extends JpaRepository<QuestionnaireSession, Long> {
    Optional<QuestionnaireSession> findByChildIdAndStatus(Long childId, SessionStatus status);
    boolean existsByChildIdAndStatus(Long childId, SessionStatus status);
    List<QuestionnaireSession> findByChildIdAndStatusOrderByCompletedAtAsc(Long childId, SessionStatus status);
}
