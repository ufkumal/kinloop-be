package com.kinloop.backend.repository;
import com.kinloop.backend.entity.ChildAnswer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChildAnswerRepository extends JpaRepository<ChildAnswer, Long> {
    List<ChildAnswer> findBySessionId(Long sessionId);
    Optional<ChildAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
