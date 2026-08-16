package com.kinloop.backend.service;

import com.kinloop.backend.dto.home.HomeActivityResponse;
import com.kinloop.backend.dto.home.HomeStatusResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.DailyPlanItem;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final ChildRepository childRepository;
    private final DailyPlanItemRepository dailyPlanItemRepository;
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

        HomeActivityResponse latestActivity = latestItem == null ? null : new HomeActivityResponse(
                latestItem.getId(),
                latestItem.getActivity().getId(),
                latestItem.getActivity().getTitle(),
                latestItem.getSelectedAt()
        );
        String childName = childService.displayName(
                currentChild, currentChild.ageInMonths(LocalDate.now()));
        return HomeStatusResponse.returningUser(currentChild.getId(), childName, latestActivity);
    }

    private Child firstCompletedChild(Long parentProfileId) {
        List<Child> children = childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(parentProfileId);
        return children.stream()
                .filter(child -> child.getOnboardingCompletedAt() != null)
                .findFirst()
                .orElse(null);
    }
}
