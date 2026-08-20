package com.kinloop.backend.mapper.profile;

import com.kinloop.backend.entity.ChildAnswer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(30)
@Component
public class NumericProfileAnswerValueResolver implements ProfileAnswerValueResolver {

    @Override
    public boolean supports(ChildAnswer answer) {
        return answer.getNumericValue() != null;
    }

    @Override
    public ResolvedProfileAnswer resolve(ChildAnswer answer) {
        String value = answer.getNumericValue().toString();
        return new ResolvedProfileAnswer(value, value);
    }
}
