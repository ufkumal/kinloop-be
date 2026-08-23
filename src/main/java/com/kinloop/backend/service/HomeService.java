package com.kinloop.backend.service;

import com.kinloop.backend.dto.home.HomeActivityResponse;
import com.kinloop.backend.dto.home.HomeFeedbackResponse;
import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.entity.Activity;
import com.kinloop.backend.entity.ActivityInstruction;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.entity.Feedback;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import com.kinloop.backend.repository.FeedbackRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChildRepository childRepository;
    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final FeedbackRepository feedbackRepository;
    private final ChildService childService;

    @Transactional(readOnly = true)
    public HomeStatusResponse getStatus(Long parentProfileId) {
        boolean onboardingCompleted =
                childRepository.existsByParentIdAndOnboardingCompletedAtIsNotNullAndDeletedAtIsNull(parentProfileId);
        if (!onboardingCompleted) {
            return HomeStatusResponse.newUser();
        }

        DailyPlanItem latestItem = dailyPlanItemRepository
                .findLatestSelectedByParentId(parentProfileId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElse(null);

        Child currentChild = latestItem == null
                ? firstCompletedChild(parentProfileId)
                : childRepository.findByIdAndParentIdAndDeletedAtIsNull(
                        latestItem.getDailyPlan().getChildId(), parentProfileId).orElse(null);

        if (currentChild == null) {
            return HomeStatusResponse.returningUser();
        }

        String childName = childService.displayName(
                currentChild, currentChild.ageInMonths(LocalDate.now()));
        if (latestItem == null) {
            return HomeStatusResponse.returningUser(currentChild.getId(), childName, null, true);
        }

        Optional<Feedback> feedback = feedbackRepository.findByChildIdAndDailyPlanItemId(
                currentChild.getId(), latestItem.getId());
        HomeActivityResponse latestActivity = activityResponse(latestItem, feedback.orElse(null));
        if (!latestItem.isCompleted()) {
            return HomeStatusResponse.feedbackRequired(currentChild.getId(), childName, latestActivity);
        }
        return HomeStatusResponse.returningUser(currentChild.getId(), childName, latestActivity, true);
    }

    private HomeActivityResponse activityResponse(DailyPlanItem item, Feedback feedback) {
        Activity activity = item.getActivity();
        ActivityInstruction instruction = activity.getInstruction();
        return new HomeActivityResponse(
                item.getId(),
                activity.getId(),
                activity.getTitle(),
                activity.getDescription(),
                activity.getDurationMinutes(),
                item.getSlotType().name(),
                instruction == null ? null : instruction.getIntro(),
                instruction == null ? null : instruction.getPurpose(),
                instruction == null ? null : instruction.getWhyItMatters(),
                instruction == null ? null : instruction.getEasierVariation(),
                instruction == null ? null : instruction.getHarderVariation(),
                instruction == null ? null : instruction.getObservationTip(),
                item.getSelectedAt(),
                item.getCompletedAt(),
                feedback != null,
                feedback == null ? null : new HomeFeedbackResponse(
                        feedback.getId(),
                        feedback.getFeedbackType(),
                        feedback.getResolvedReason(),
                        feedback.getFreeText(),
                        feedback.getCreatedAt()));
    }

    private Child firstCompletedChild(Long parentProfileId) {
        List<Child> children = childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(parentProfileId);
        return children.stream()
                .filter(child -> child.getOnboardingCompletedAt() != null)
                .findFirst()
                .orElse(null);
    }
}
