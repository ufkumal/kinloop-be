package com.kinloop.backend.dto.matching;

public record ActivityMaterialResponse(
        String name,
        String category,
        String quantity,
        boolean optional,
        int displayOrder,
        String note
) {
}
