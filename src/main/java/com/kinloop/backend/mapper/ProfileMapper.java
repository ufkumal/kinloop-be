package com.kinloop.backend.mapper;

import com.kinloop.backend.dto.profile.ActivityHistoryItemResponse;
import com.kinloop.backend.dto.profile.ChildProfileResponse;
import com.kinloop.backend.dto.profile.ParentProfileDetailsResponse;
import com.kinloop.backend.dto.profile.ProfileAnswerResponse;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildAnswer;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.QuestionOption;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.mapper.profile.ProfileAnswerValueResolver;
import com.kinloop.backend.mapper.profile.ResolvedProfileAnswer;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileMapper {

    private final List<ProfileAnswerValueResolver> answerValueResolvers;

    public ParentProfileDetailsResponse toParentResponse(ParentProfile profile, User user) {
        return new ParentProfileDetailsResponse(
                profile.getId(),
                user.getEmail(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getCity(),
                profile.getDistrict(),
                profile.getDailyTimeBudgetMinutes());
    }

    public ChildProfileResponse toChildResponse(
            Child child,
            String displayName,
            List<ProfileAnswerResponse> answers,
            LocalDate today) {
        return new ChildProfileResponse(
                child.getId(),
                child.getFullName(),
                displayName,
                child.getBirthDate(),
                child.ageInMonths(today),
                child.getGender(),
                child.getPreferenceMode(),
                child.getOnboardingCompletedAt(),
                answers);
    }

    public ProfileAnswerResponse toAnswerResponse(ChildAnswer answer) {
        QuestionOption option = answer.getOption();
        ResolvedProfileAnswer resolvedAnswer = answerValueResolvers.stream()
                .filter(resolver -> resolver.supports(answer))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Answer has no supported value: " + answer.getId()))
                .resolve(answer);
        return new ProfileAnswerResponse(
                answer.getQuestion().getCode(),
                answer.getQuestion().getBody(),
                answer.getQuestion().getQuestionType(),
                answer.getQuestion().getDisplayOrder(),
                option == null ? null : option.getCode(),
                resolvedAnswer.value(),
                resolvedAnswer.displayValue(),
                answer.getAnsweredAt());
    }

    public ActivityHistoryItemResponse toActivityHistoryResponse(DailyPlanItem item) {
        Activity activity = item.getActivity();
        return new ActivityHistoryItemResponse(
                item.getId(),
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getDurationMinutes(),
                activity.getTargetDomain(),
                activity.getInvolvementType(),
                item.getDailyPlan().getPlanDate(),
                item.getSelectedAt(),
                item.getCompletedAt(),
                item.isCompleted());
    }

}
