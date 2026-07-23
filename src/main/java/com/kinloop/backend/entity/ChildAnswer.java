package com.kinloop.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter @NoArgsConstructor
@Entity @Table(name = "child_answers")
public class ChildAnswer {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "child_id", nullable = false) private Long childId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "session_id") private QuestionnaireSession session;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id") private Question question;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "question_option_id") private QuestionOption option;
    @JdbcTypeCode(SqlTypes.SMALLINT) @Column(name = "numeric_value") private Integer numericValue;
    @Column(name = "text_value", columnDefinition = "TEXT") private String textValue;
    @Column(name = "date_value") private LocalDate dateValue;
    @Column(name = "answered_at", nullable = false) private OffsetDateTime answeredAt;

    public static ChildAnswer ofOption(QuestionnaireSession session, Question question, QuestionOption option) {
        ChildAnswer answer = base(session, question); answer.option = option; return answer;
    }
    public static ChildAnswer ofDate(QuestionnaireSession session, Question question, LocalDate date) {
        ChildAnswer answer = base(session, question); answer.dateValue = date; return answer;
    }
    private static ChildAnswer base(QuestionnaireSession session, Question question) {
        ChildAnswer answer = new ChildAnswer(); answer.session = session; answer.childId = session.getChildId();
        answer.question = question; answer.answeredAt = OffsetDateTime.now(); return answer;
    }
    public void replaceWithOption(QuestionOption option) {
        this.option = option; numericValue = null; textValue = null; dateValue = null; answeredAt = OffsetDateTime.now();
    }
    public void replaceWithDate(LocalDate date) {
        option = null; numericValue = null; textValue = null; dateValue = date; answeredAt = OffsetDateTime.now();
    }
}
