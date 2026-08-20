package com.kinloop.backend.dto.profile;

import java.util.List;

public record ProfileResponse(
        ParentProfileDetailsResponse parent,
        List<ChildProfileResponse> children
) {
}
