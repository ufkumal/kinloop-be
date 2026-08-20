package com.kinloop.backend.mapper.profile;

import com.kinloop.backend.entity.ChildAnswer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(40)
@Component
public class DateProfileAnswerValueResolver implements ProfileAnswerValueResolver {

    @Override
    public boolean supports(ChildAnswer answer) {
        return answer.getDateValue() != null;
    }

    @Override
    public ResolvedProfileAnswer resolve(ChildAnswer answer) {
        String value = answer.getDateValue().toString();
        return new ResolvedProfileAnswer(value, value);
    }
}
