package com.kinloop.backend.mapper.profile;

import com.kinloop.backend.entity.ChildAnswer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(20)
@Component
public class TextProfileAnswerValueResolver implements ProfileAnswerValueResolver {

    @Override
    public boolean supports(ChildAnswer answer) {
        return answer.getTextValue() != null;
    }

    @Override
    public ResolvedProfileAnswer resolve(ChildAnswer answer) {
        return new ResolvedProfileAnswer(answer.getTextValue(), answer.getTextValue());
    }
}
