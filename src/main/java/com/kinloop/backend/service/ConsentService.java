package com.kinloop.backend.service;

import com.kinloop.backend.dto.consent.ConsentResponse;
import com.kinloop.backend.entity.ConsentDocument;
import com.kinloop.backend.entity.User;
import com.kinloop.backend.entity.UserConsent;
import com.kinloop.backend.entity.enums.ConsentType;
import com.kinloop.backend.exception.RequiredConsentMissingException;
import com.kinloop.backend.repository.ConsentDocumentRepository;
import com.kinloop.backend.repository.UserConsentRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConsentService {
    private final ConsentDocumentRepository consentDocumentRepository;
    private final UserConsentRepository userConsentRepository;

    @Transactional(readOnly = true)
    public List<ConsentResponse> getActiveConsents(Long userId) {
        List<ConsentDocument> documents = consentDocumentRepository.findByActiveTrueOrderByDisplayOrderAsc();
        List<Long> ids = documents.stream().map(ConsentDocument::getId).toList();
        Map<Long, UserConsent> decisions = ids.isEmpty() ? Map.of() : userConsentRepository
                .findByUserIdAndConsentDocumentIdIn(userId, ids).stream()
                .collect(Collectors.toMap(c -> c.getConsentDocument().getId(), Function.identity()));
        return documents.stream().map(document -> toResponse(document, decisions.get(document.getId()))).toList();
    }

    @Transactional
    public ConsentResponse updateConsent(User user, Long consentId, boolean granted) {
        ConsentDocument document = consentDocumentRepository.findById(consentId)
                .filter(ConsentDocument::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Active consent not found: " + consentId));
        OffsetDateTime now = OffsetDateTime.now();
        UserConsent decision = userConsentRepository.findByUserIdAndConsentDocumentId(user.getId(), consentId)
                .orElseGet(UserConsent::new);
        if (decision.getId() == null) {
            decision.setUser(user);
            decision.setConsentDocument(document);
        }
        decision.setRespondedAt(now);
        decision.setGranted(granted);
        decision.setRevokedAt(granted ? null : now);
        decision.setUpdatedAt(now);
        return toResponse(document, userConsentRepository.save(decision));
    }

    @Transactional(readOnly = true)
    public boolean hasGrantedAllRequiredConsents(Long userId) {
        return firstMissingRequiredConsentId(userId).isEmpty();
    }

    @Transactional(readOnly = true)
    public Optional<Long> firstMissingRequiredConsentId(Long userId) {
        List<ConsentDocument> requiredDocuments = consentDocumentRepository
                .findByActiveTrueOrderByDisplayOrderAsc().stream()
                .filter(ConsentDocument::isRequired)
                .toList();
        if (requiredDocuments.isEmpty()) {
            return Optional.empty();
        }
        List<Long> requiredIds = requiredDocuments.stream().map(ConsentDocument::getId).toList();
        Map<Long, UserConsent> decisions = userConsentRepository
                .findByUserIdAndConsentDocumentIdIn(userId, requiredIds).stream()
                .collect(Collectors.toMap(c -> c.getConsentDocument().getId(), Function.identity()));
        return requiredIds.stream().filter(id -> {
            UserConsent decision = decisions.get(id);
            return decision == null || !decision.isGranted() || decision.getRevokedAt() != null;
        }).findFirst();
    }

    @Transactional(readOnly = true)
    public void requireAllRequiredConsents(Long userId) {
        if (!hasGrantedAllRequiredConsents(userId)) {
            throw new RequiredConsentMissingException();
        }
    }

    @Transactional(readOnly = true)
    public boolean hasGrantedConsent(Long userId, ConsentType type) {
        return consentDocumentRepository.findFirstByTypeAndActiveTrue(type)
                .map(document -> userConsentRepository
                        .existsByUserIdAndConsentDocumentIdAndGrantedTrue(userId, document.getId()))
                .orElse(false);
    }

    private ConsentResponse toResponse(ConsentDocument document, UserConsent decision) {
        return new ConsentResponse(
                document.getId(), document.getType(), document.getVersion(), document.getTitle(),
                document.getSummary(), document.getContent(), document.isRequired(), document.getEffectiveAt(),
                decision == null ? null : decision.isGranted(),
                decision == null ? null : decision.getRespondedAt());
    }
}
