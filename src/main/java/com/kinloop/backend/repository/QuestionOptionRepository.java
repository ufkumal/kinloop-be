package com.kinloop.backend.repository;
import com.kinloop.backend.entity.QuestionOption;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Long> {
    Optional<QuestionOption> findByQuestionIdAndCode(Long questionId, String code);
}
