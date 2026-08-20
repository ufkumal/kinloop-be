package com.kinloop.backend.service;

import com.kinloop.backend.dto.profile.ChildProfileResponse;
import com.kinloop.backend.dto.profile.ProfileAnswerResponse;
import com.kinloop.backend.dto.profile.ProfileResponse;
import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.ChildAnswer;
import com.kinloop.backend.entity.ParentProfile;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.entity.enums.SessionStatus;
import com.kinloop.backend.mapper.ProfileMapper;
import com.kinloop.backend.repository.ChildAnswerRepository;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.repository.ParentProfileRepository;
import com.kinloop.backend.repository.UserRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ParentProfileRepository parentProfileRepository;
    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final ChildAnswerRepository childAnswerRepository;
    private final ChildService childService;
    private final ProfileMapper profileMapper;

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long parentProfileId) {
        ParentProfile parent = parentProfileRepository.findById(parentProfileId)
                .filter(profile -> profile.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalStateException("Parent profile not found"));
        User user = userRepository.findById(parent.getUserId())
                .filter(candidate -> candidate.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalStateException("Parent user not found"));
        List<Child> children = childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(parentProfileId);
        Map<Long, List<ProfileAnswerResponse>> answersByChild = latestAnswersByChild(children);
        LocalDate today = LocalDate.now();

        List<ChildProfileResponse> childResponses = children.stream()
                .map(child -> profileMapper.toChildResponse(
                        child,
                        childService.displayName(child, child.ageInMonths(today)),
                        answersByChild.getOrDefault(child.getId(), List.of()),
                        today))
                .toList();
        return new ProfileResponse(profileMapper.toParentResponse(parent, user), childResponses);
    }

    private Map<Long, List<ProfileAnswerResponse>> latestAnswersByChild(List<Child> children) {
        if (children.isEmpty()) {
            return Map.of();
        }
        List<Long> childIds = children.stream().map(Child::getId).toList();
        List<ChildAnswer> answers = childAnswerRepository.findByChildIdsAndSessionStatus(
                childIds, SessionStatus.COMPLETED);

        Map<Long, LinkedHashMap<Long, ProfileAnswerResponse>> latest = new LinkedHashMap<>();
        for (ChildAnswer answer : answers) {
            latest.computeIfAbsent(answer.getChildId(), ignored -> new LinkedHashMap<>())
                    .putIfAbsent(answer.getQuestion().getId(), profileMapper.toAnswerResponse(answer));
        }

        Map<Long, List<ProfileAnswerResponse>> result = new LinkedHashMap<>();
        latest.forEach((childId, byQuestion) -> result.put(
                childId,
                byQuestion.values().stream()
                        .sorted(Comparator.comparingInt(ProfileAnswerResponse::displayOrder))
                        .toList()));
        return result;
    }
}
