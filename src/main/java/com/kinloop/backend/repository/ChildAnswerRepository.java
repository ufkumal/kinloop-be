package com.kinloop.backend.repository;
import com.kinloop.backend.entity.ChildAnswer;
import com.kinloop.backend.entity.enums.SessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface ChildAnswerRepository extends JpaRepository<ChildAnswer, Long> {
    List<ChildAnswer> findBySessionId(Long sessionId);
    Optional<ChildAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);

    @EntityGraph(attributePaths = {"session", "question", "option"})
    @Query("""
            select answer
            from ChildAnswer answer
            where answer.childId in :childIds
              and answer.session.status = :status
            order by answer.answeredAt desc, answer.id desc
            """)
    List<ChildAnswer> findByChildIdsAndSessionStatus(
            @Param("childIds") List<Long> childIds,
            @Param("status") SessionStatus status);
}
