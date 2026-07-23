package com.kinloop.backend.service;

import com.kinloop.backend.entity.Child;
import com.kinloop.backend.entity.enums.PreferenceMode;
import com.kinloop.backend.repository.ChildRepository;
import com.kinloop.backend.dto.child.CreateChildRequest;
import com.kinloop.backend.dto.child.CreateChildResponse;
import com.kinloop.backend.dto.child.SessionSummaryResponse;
import com.kinloop.backend.exception.UnsupportedChildAgeException;
import com.kinloop.backend.exception.ChildNotFoundException;
import com.kinloop.backend.entity.QuestionnaireSession;
import com.kinloop.backend.entity.enums.AgeBand;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChildService {

    private static final String BABY_DISPLAY_NAME = "Bebeğiniz";
    private static final String CHILD_DISPLAY_NAME = "Çocuğunuz";

    private final ChildRepository childRepository;
    private final QuestionnaireSessionService questionnaireSessionService;

    @Transactional
    public CreateChildResponse createChild(Long parentProfileId, CreateChildRequest request) {
        String fullName = trimToNull(request.fullName());
        int ageMonths = AgeBand.ageInMonths(request.birthDate(), LocalDate.now());
        if (!AgeBand.isSupported(ageMonths)) {
            throw new UnsupportedChildAgeException(ageMonths);
        }

        Child child = new Child();
        child.setParentId(parentProfileId);
        child.setFullName(fullName);
        child.setBirthDate(request.birthDate());
        child.setPreferenceMode(PreferenceMode.BALANCED);

        Child savedChild = childRepository.save(child);
        QuestionnaireSession session = questionnaireSessionService.openInitialSession(savedChild.getId(), ageMonths);
        SessionSummaryResponse sessionSummary = questionnaireSessionService.summarise(session, ageMonths, 0);

        return new CreateChildResponse(
                savedChild.getId(),
                savedChild.getFullName(),
                displayName(savedChild, ageMonths),
                savedChild.getBirthDate(),
                ageMonths,
                AgeBand.resolve(ageMonths),
                sessionSummary
        );
    }

    @Transactional(readOnly = true)
    public Child getOwnedChild(Long childId, Long parentProfileId) {
        return childRepository.findByIdAndParentIdAndDeletedAtIsNull(childId, parentProfileId)
                .orElseThrow(() -> new ChildNotFoundException(childId));
    }

    /**
     * Preserves null in storage for intentionally omitted names and computes a stable UI label per request.
     */
    public String displayName(Child child, int ageMonths) {
        if (child.getFullName() != null && !child.getFullName().isBlank()) {
            return child.getFullName();
        }

        String base = ageMonths < 24 ? BABY_DISPLAY_NAME : CHILD_DISPLAY_NAME;
        long unnamedCount = childRepository.countByParentIdAndFullNameIsNullAndDeletedAtIsNull(child.getParentId());
        if (unnamedCount <= 1) {
            return base;
        }

        List<Child> unnamedChildren = childRepository.findByParentIdAndDeletedAtIsNullOrderByIdAsc(child.getParentId())
                .stream()
                .filter(sibling -> sibling.getFullName() == null)
                .toList();

        for (int i = 0; i < unnamedChildren.size(); i++) {
            if (unnamedChildren.get(i).getId().equals(child.getId())) {
                return base + " " + (i + 1);
            }
        }
        return base;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
