package com.kinloop.backend.repository;

import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.enums.SessionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionnaireSessionRepository extends JpaRepository<QuestionnaireSession, Long> {

    Optional<QuestionnaireSession> findByChildIdAndStatus(Long childId, SessionStatus status);
}
