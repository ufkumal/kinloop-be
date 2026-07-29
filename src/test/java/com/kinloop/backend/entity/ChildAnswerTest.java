package com.kinloop.backend.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChildAnswerTest {

    @Test
    void storesTextOptionCodeInTextValue() {
        QuestionOption option = new QuestionOption();
        option.setCode("D");
        ChildAnswer answer = new ChildAnswer();

        answer.replaceWithOption(option);

        assertThat(answer.getOption()).isSameAs(option);
        assertThat(answer.getTextValue()).isEqualTo("D");
        assertThat(answer.getNumericValue()).isNull();
    }

    @Test
    void storesNumericOptionCodeInNumericValue() {
        QuestionOption option = new QuestionOption();
        option.setCode("4");
        ChildAnswer answer = new ChildAnswer();

        answer.replaceWithOption(option);

        assertThat(answer.getOption()).isSameAs(option);
        assertThat(answer.getNumericValue()).isEqualTo(4);
        assertThat(answer.getTextValue()).isNull();
    }
}
