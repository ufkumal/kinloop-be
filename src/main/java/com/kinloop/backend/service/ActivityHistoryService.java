package com.kinloop.backend.service;

import com.kinloop.backend.dto.profile.ActivityHistoryItemResponse;
import com.kinloop.backend.dto.profile.ActivityHistoryResponse;
import com.kinloop.backend.mapper.ProfileMapper;
import com.kinloop.backend.repository.DailyPlanItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityHistoryService {

    private final DailyPlanItemRepository dailyPlanItemRepository;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public ActivityHistoryResponse getSelectedHistory(Long childId, int page, int size) {
        Slice<com.kinloop.backend.entity.DailyPlanItem> history = dailyPlanItemRepository
                .findSelectedHistoryByChildId(childId, PageRequest.of(page, size));
        List<ActivityHistoryItemResponse> activities = history.getContent().stream()
                .map(profileMapper::toActivityHistoryResponse)
                .toList();
        return new ActivityHistoryResponse(childId, page, size, history.hasNext(), activities);
    }
}
