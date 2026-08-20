package com.kinloop.backend.mapper.profile;

import com.kinloop.backend.entity.ChildAnswer;

public interface ProfileAnswerValueResolver {

    boolean supports(ChildAnswer answer);

    ResolvedProfileAnswer resolve(ChildAnswer answer);
}
