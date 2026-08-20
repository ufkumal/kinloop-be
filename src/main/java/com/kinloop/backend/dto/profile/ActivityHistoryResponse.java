package com.kinloop.backend.dto.profile;

import java.util.List;

public record ActivityHistoryResponse(
        Long childId,
        int page,
        int size,
        boolean hasNext,
        List<ActivityHistoryItemResponse> activities
) {
}
