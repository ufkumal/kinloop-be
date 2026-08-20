package com.kinloop.backend.mapper.profile;

import com.kinloop.backend.entity.ChildAnswer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(10)
@Component
public class OptionProfileAnswerValueResolver implements ProfileAnswerValueResolver {

    @Override
    public boolean supports(ChildAnswer answer) {
        return answer.getOption() != null;
    }

    @Override
    public ResolvedProfileAnswer resolve(ChildAnswer answer) {
        return new ResolvedProfileAnswer(answer.getOption().getCode(), answer.getOption().getLabel());
    }
}
