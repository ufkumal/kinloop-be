package com.kinloop.backend.repository;

import com.kinloop.backend.entity.Question;
import com.kinloop.backend.entity.enums.QuestionScope;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByScopeAndActiveTrueOrderByDisplayOrderAsc(QuestionScope scope);

    /**
     * Uses an explicit upper-bound branch so age 12 matches only Q2 (12-72) and not Q2b (0-12),
     * while still keeping the 72-month ceiling inclusive for BAND_48_72.
     */
    @Query("""
        SELECT q FROM Question q
         WHERE q.active = true
           AND q.scope = :scope
           AND :ageMonths >= q.minAgeMonths
           AND (:ageMonths < q.maxAgeMonths
                OR (:ageMonths = q.maxAgeMonths AND q.maxAgeMonths = 72))
         ORDER BY q.displayOrder ASC
        """)
    List<Question> findForAge(@Param("ageMonths") int ageMonths, @Param("scope") QuestionScope scope);
}
